package com.vegabond.antispymode.FaceRecognition_Module;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.vegabond.antispymode.MainActivity;
import com.vegabond.antispymode.R;

import static com.vegabond.antispymode.FaceRecognition_Module.training_Main.currentFace;

public class Training extends AppCompatActivity {

    Button btnTraining;
    String name;
    ToggleButton capture;
    int totalFaces;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        btnTraining = findViewById(R.id.btnTrain);

        Intent intent = getIntent();
        name = intent.getStringExtra("Name");
        totalFaces = Integer.parseInt(MainActivity.settingControl.getNoOfFacesInFaceRecognition());

        btnTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (currentFace<totalFaces){
                    currentFace++;

                    Toast.makeText(getApplicationContext(),"Trained Face for "+name,Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Training.this,training_Main.class));

                }else{
                    currentFace = 1;
                    Toast.makeText(getApplicationContext(),"Trained Face for "+name,Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Training.this,MainActivity.class));

                }
            }
        });


        capture = (ToggleButton) findViewById(R.id.capture);
        capture.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                captureOnClick();
            }
        });
    }

    void captureOnClick()
    {
        if (capture.isChecked())
            Toast.makeText(this, "Capturing", Toast.LENGTH_SHORT).show();
        else {
            Toast.makeText(this, "Captured", Toast.LENGTH_SHORT).show();

        }
    }
}
