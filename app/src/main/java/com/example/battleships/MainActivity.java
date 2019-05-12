package com.example.battleships;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText playerNameTextField;
    private Intent intent;
    private boolean btAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerNameTextField = findViewById(R.id.player_name_input);
        intent = new Intent(this, PlayActivity.class);
    }

    public void launchPlayActivity() {
//        if(playerNameTextField.getText().toString().equals("")){
//            Toast toast = Toast.makeText(this, "Please enter a name!", Toast.LENGTH_LONG);
//            toast.show();
//            return;
//        }
        intent.putExtra("PlayerName", playerNameTextField.getText().toString());
        startActivity(intent);
    }

    public void setOffline(View view) {
        intent.putExtra("HostType", 0);
        launchPlayActivity();
    }

    public void setHosting(View view) {
        intent.putExtra("HostType", 1);
        launchPlayActivity();

    }

    public void setConnecting(View view) {
        intent.putExtra("HostType", 2);
        launchPlayActivity();
    }
}
