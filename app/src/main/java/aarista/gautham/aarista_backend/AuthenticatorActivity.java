package aarista.gautham.aarista_backend;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.amazonaws.mobile.auth.ui.SignInUI;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;

public class AuthenticatorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
                SignInUI signin = (SignInUI) AWSMobileClient.getInstance().getClient(
                        AuthenticatorActivity.this,SignInUI.class
                );
                signin.login(AuthenticatorActivity.this,MainActivity.class).execute();
            }
        }).execute();

    }

}
