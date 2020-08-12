package com.example.escaperoomcontroller;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    static final String EXTRA_IP = "EXTRA_IP";
    static final String EXTRA_PORT = "EXTRA_PORT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startGame(View view) {
        Intent i = new Intent(this, GameActivity.class);

        i.putExtra(EXTRA_IP, ((EditText) findViewById(R.id.server_ip)).getText().toString());
        i.putExtra(EXTRA_PORT, ((EditText) findViewById(R.id.server_port)).getText().toString());;

        startActivity(i);
    }
}