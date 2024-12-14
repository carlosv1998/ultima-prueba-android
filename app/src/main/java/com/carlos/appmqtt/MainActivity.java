package com.carlos.appmqtt;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class MainActivity extends AppCompatActivity {

    private EditText txtNombre, txtCorreo, txtContra, txtRut;
    private TextView textView;
    private ListView lista;
    private Button boton, botonCargar;
    private FirebaseFirestore db;

    //mqtt
    private static String mqttHost = "tcp://mqttcarlos.cloud.shiftr.io:1883";
    private static String idUsuario = "galaxy";
    private static String topico = "Mensaje";
    private static String user = "mqttcarlos";
    private static String pass = "YiWbspdzY4nMf08q";

    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        boton = findViewById(R.id.boton);
        botonCargar = findViewById(R.id.botonCargar);
        lista = findViewById(R.id.lista);

        txtNombre = findViewById(R.id.nombre);
        txtCorreo = findViewById(R.id.correo);
        txtContra = findViewById(R.id.contra);
        txtRut = findViewById(R.id.rut);

        textView = findViewById(R.id.textView);

        boton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarDatosFireStore();
            }
        });


        cargarListaFireStore();

        try{
            mqttClient = new MqttClient(mqttHost, idUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(user);
            options.setPassword(pass.toCharArray());

            mqttClient.connect(options);
            Toast.makeText(this, "Aplicación conectada al servidor MQTT", Toast.LENGTH_SHORT).show();

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT", "Conexión perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    runOnUiThread(() -> textView.setText(payload));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Entrega completa");
                }
            });


        }catch(MqttException e){
            e.printStackTrace();
        }

    }


    public void enviarDatosFireStore(){
        String rut = txtRut.getText().toString();
        String nombre = txtNombre.getText().toString();
        String correo = txtCorreo.getText().toString();
        String contra = txtContra.getText().toString();

        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put("Rut", rut);
        datosUsuario.put("Nombre", nombre);
        datosUsuario.put("Correo", correo);
        datosUsuario.put("Contra", contra);



        db.collection("usuarios")
            .document(rut)
            .set(datosUsuario)
            .addOnSuccessListener(s -> {
                Toast.makeText(MainActivity.this, "Datos enviados a FireStore correctamente", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(MainActivity.this, "Error al enviar los datos a FireStore", Toast.LENGTH_SHORT).show();
            });


        try {
            String mensaje = rut;
            if (mqttClient != null && mqttClient.isConnected()){
                mqttClient.publish(topico, mensaje.getBytes(), 0, false);
                textView.setText(mensaje);
                Toast.makeText(MainActivity.this, "Mensaje enviado", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(MainActivity.this, "No se pudo enviar el mensaje", Toast.LENGTH_SHORT).show();
            }
        }catch(MqttException e){
            e.printStackTrace();
        }

    }

    public void cargarLista(View view){
        cargarListaFireStore();
    }

    public void cargarListaFireStore(){
        FirebaseFirestore bd = FirebaseFirestore.getInstance();
        db.collection("usuarios")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            List<String> listaUsuarios = new ArrayList<>();

                            for (QueryDocumentSnapshot document: task.getResult()){
                                String linea =
                                        "Rut:" + document.getString("Rut") + "|" +
                                        "Nombre" + document.getString("Nombre") + "|" +
                                        "Correo" + document.getString("Correo");

                                listaUsuarios.add(linea);
                            }
                            ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                                    MainActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    listaUsuarios
                            );

                            lista.setAdapter(adaptador);


                        }else {
                            Log.e("Tag", "Error al obtener los datos de FireStore", task.getException());
                        }
                    }
                });
    }

}