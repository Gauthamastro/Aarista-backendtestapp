package aarista.gautham.aarista_backend;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobile.api.idomfh8ovf90.InsertUserdataMobileHubClient;
import com.amazonaws.mobile.api.id5a8xcsgpe5.GetDeviceStateMobileHubClient;
import com.amazonaws.mobile.api.idj8qkx2lcr5.SendCommandstoDeviceMobileHubClient;
import com.amazonaws.mobile.api.idjiowot8h4g.AuthvkeyMobileHubClient;
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory;
import com.amazonaws.mobileconnectors.apigateway.ApiRequest;
import com.amazonaws.mobileconnectors.apigateway.ApiResponse;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    final String TAG = "USERDETAILS";
    private InsertUserdataMobileHubClient apiclient;
    private GetDeviceStateMobileHubClient getdevicestateapiclient;
    private SendCommandstoDeviceMobileHubClient sendCommandstoDeviceMobileHubClient;
    private AuthvkeyMobileHubClient vkeyapiclient;
    String user_id;
    String phone_number ;
    String name ;
    String email;
    String cmdtext ;
    JSONObject userdata;
    TextView display ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = (TextView)findViewById(R.id.responseDisplay);
        Button getapi = (Button)findViewById(R.id.getapi);
        getapi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callinsertuser();
            }
        });
        Button getState =(Button) findViewById(R.id.getDeviceStatebtn);
        final EditText cmds = (EditText)findViewById(R.id.editText);
        getState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cmdtext = String.valueOf(cmds.getText());
                getDeviceState();
            }
        });
        Button sendcmdsbtn = (Button)findViewById(R.id.sendCommandsbtn);
        sendcmdsbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cmdtext = String.valueOf(cmds.getText());
                sendCommands();
            }
        });
        Button sendvkey = (Button) findViewById(R.id.snedvkey);
        sendvkey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cmdtext = String.valueOf(cmds.getText());
                sendVirtualkey();
            }
        });
        apiclient = new ApiClientFactory()
                .credentialsProvider(AWSMobileClient.getInstance().getCredentialsProvider())
                .build(InsertUserdataMobileHubClient.class);

        getdevicestateapiclient = new ApiClientFactory()
                .credentialsProvider(AWSMobileClient.getInstance().getCredentialsProvider())
                .build(GetDeviceStateMobileHubClient.class);

        sendCommandstoDeviceMobileHubClient = new ApiClientFactory()
                .credentialsProvider(AWSMobileClient.getInstance().getCredentialsProvider())
                .build(SendCommandstoDeviceMobileHubClient.class);
        vkeyapiclient = new ApiClientFactory()
                .credentialsProvider(AWSMobileClient.getInstance().getCredentialsProvider())
                .build(AuthvkeyMobileHubClient.class);

        CognitoUserPool userPool = new CognitoUserPool(this,AWSMobileClient.getInstance().getConfiguration());
        Log.d(TAG,userPool.toString());
        CognitoUser user = userPool.getCurrentUser();
        Log.d(TAG,user.getUserId());
        GetDetailsHandler handler = new GetDetailsHandler() {
            @Override
            public void onSuccess(CognitoUserDetails cognitoUserDetails) {
                Log.d(TAG,"Please Wait printing details");
                Map<String, String> userdetailsmap = cognitoUserDetails.getAttributes().getAttributes(); // This contains all the details regarding the user acc use this to upload to database
                user_id = userdetailsmap.get("sub");
                phone_number = userdetailsmap.get("phone_number");
                name = userdetailsmap.get("given_name");
                email = userdetailsmap.get("email");
                Log.d(TAG, user_id);
                userdata = new JSONObject(userdetailsmap);
                Log.d(TAG,userdata.toString());
            }

            @Override
            public void onFailure(Exception exception) {
                Log.d(TAG,"Failed tp retrieve User Details");
            }
        };
        user.getDetailsInBackground(handler);

    }

    private void sendVirtualkey() {
        String[] cmdarglist = cmdtext.split(",");
        String senders_name = cmdarglist[0];
        String number = cmdarglist[1];
        String hardware_id = cmdarglist[2];

        final String method = "POST";
        final String path = "/items";
        final Map datamap = new HashMap<>();
        datamap.put("user_id",user_id);
        datamap.put("type","send-virtual-key");
        datamap.put("time",System.currentTimeMillis()/1000L);
        datamap.put("hardware_id",hardware_id);
        datamap.put("name",senders_name);
        datamap.put("phone_number",number);
        datamap.put("valid_till",System.currentTimeMillis()/1000L + 1000000);

        JSONObject data = new JSONObject(datamap);
        final String body = data.toString();
        final byte[] content = body.getBytes(StringUtils.UTF8);
        final Map parameters = new HashMap<>();
        parameters.put("lang","en_US");
        final Map headers = new HashMap<>();


        ApiRequest localrequest = new ApiRequest(sendCommandstoDeviceMobileHubClient.getClass().getSimpleName())
                .withPath(path)
                .withHttpMethod(HttpMethodName.valueOf(method))
                .withHeaders(headers)
                .addHeader("Content-Type","application/json")
                .withParameters(parameters);

        if(body.length()>0){
            localrequest = localrequest.addHeader("Content-Length",String.valueOf(content.length))
                    .withBody(content);
        }
        final  ApiRequest request = localrequest;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Log.d(TAG,"Invoking API Request : "+ request.getHttpMethod()+":"+request.getPath());
                    final ApiResponse response = sendCommandstoDeviceMobileHubClient.execute(request);
                    final InputStream responecontentstream = response.getContent();
                    if (responecontentstream !=null){
                        final String responseData = IOUtils.toString(responecontentstream);
                        Log.d(TAG,"Send Commands API Response :"+responseData);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                display.setText(responseData);
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage(),e);
                    e.printStackTrace();
                }
            }
        }).start();




    }

    private void sendCommands() {
        final String method = "POST";
        final String path = "/items";
        final Map datamap = new HashMap<>();
        String[] cmdinput = cmdtext.split(",");
        datamap.put("user_id",user_id);
        datamap.put("type",cmdinput[1]);
        datamap.put("time",System.currentTimeMillis()/1000L);
        datamap.put("hardware_id",cmdinput[0]);

        JSONObject data = new JSONObject(datamap);
        final String body = data.toString();
        final byte[] content = body.getBytes(StringUtils.UTF8);
        final Map parameters = new HashMap<>();
        parameters.put("lang","en_US");
        final Map headers = new HashMap<>();


        ApiRequest localrequest = new ApiRequest(sendCommandstoDeviceMobileHubClient.getClass().getSimpleName())
                .withPath(path)
                .withHttpMethod(HttpMethodName.valueOf(method))
                .withHeaders(headers)
                .addHeader("Content-Type","application/json")
                .withParameters(parameters);

        if(body.length()>0){
            localrequest = localrequest.addHeader("Content-Length",String.valueOf(content.length))
                    .withBody(content);
        }
        final  ApiRequest request = localrequest;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Log.d(TAG,"Invoking API Request : "+ request.getHttpMethod()+":"+request.getPath());
                    final ApiResponse response = sendCommandstoDeviceMobileHubClient.execute(request);
                    final InputStream responecontentstream = response.getContent();
                    if (responecontentstream !=null){
                        final String responseData = IOUtils.toString(responecontentstream);
                        Log.d(TAG,"Send Commands API Response :"+responseData);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                display.setText(responseData);
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage(),e);
                    e.printStackTrace();
                }
            }
        }).start();


    }

    private void getDeviceState(){
        final String method = "POST";
        final String path = "/items";
        final Map datamap = new HashMap<>();
        datamap.put("user_id",user_id);
        datamap.put("hardware_id",cmdtext);
        JSONObject data = new JSONObject(datamap);
        final String body = data.toString();
        final byte[] content = body.getBytes(StringUtils.UTF8);
        final Map parameters = new HashMap<>();
        parameters.put("lang","en_US");
        final Map headers = new HashMap<>();

        ApiRequest localrequest = new ApiRequest(getdevicestateapiclient.getClass().getSimpleName())
                .withPath(path)
                .withHttpMethod(HttpMethodName.valueOf(method))
                .withHeaders(headers)
                .addHeader("Content-Type","application/json")
                .withParameters(parameters);

        if(body.length()>0){
            localrequest = localrequest.addHeader("Content-Length",String.valueOf(content.length))
                    .withBody(content);
        }
        final  ApiRequest request = localrequest;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Log.d(TAG,"Invoking API Request : "+ request.getHttpMethod()+":"+request.getPath());
                    final ApiResponse response = getdevicestateapiclient.execute(request);
                    final InputStream responecontentstream = response.getContent();
                    if (responecontentstream !=null){
                        final String responseData = IOUtils.toString(responecontentstream);
                        Log.d(TAG,"Get Device State Response :"+responseData);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                display.setText(responseData);
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage(),e);
                    e.printStackTrace();
                }
            }
        }).start();


    }

    private void callinsertuser(){
        final String method = "POST";

        final String path ="/items";
        final String body = userdata.toString() ;
        final byte[] content = body.getBytes(StringUtils.UTF8);

        final Map parameters = new HashMap<>();
        parameters.put("lang","en_US");
        final Map headers = new HashMap<>();

        ApiRequest localrequest = new ApiRequest(apiclient.getClass().getSimpleName())
                .withPath(path)
                .withHttpMethod(HttpMethodName.valueOf(method))
                .withHeaders(headers)
                .addHeader("Content-Type","application/json")
                .withParameters(parameters);

        if(body.length()>0){
            localrequest = localrequest.addHeader("Content-Length",String.valueOf(content.length))
                    .withBody(content);
        }
        final  ApiRequest request = localrequest;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Log.d(TAG,"Invoking API Request : "+ request.getHttpMethod()+":"+request.getPath());
                    final ApiResponse response = apiclient.execute(request);
                    final InputStream responecontentstream = response.getContent();
                    if (responecontentstream !=null){
                        final String responseData = IOUtils.toString(responecontentstream);
                        Log.d(TAG,"Response :"+responseData);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                display.setText(responseData);
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage(),e);
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
