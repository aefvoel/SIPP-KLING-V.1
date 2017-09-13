package tiregdev.sipepikeling.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import tiregdev.sipepikeling.R;
import tiregdev.sipepikeling.jenis_sab;
import tiregdev.sipepikeling.model.KK;
import tiregdev.sipepikeling.model.RS;
import tiregdev.sipepikeling.utils.SessionString;

public class RSActivity extends AppCompatActivity {

    EditText frmNamaKK, frmAlamat, frmJmlAnggota, frmNoRumah;
    Spinner spinnerRT, spinnerRW;
    Button btnSend;
    SimpleDateFormat sdf;
    RadioGroup[] rg = new RadioGroup[17];
    RadioGroup rg22, rg21, rg20, rg19;
    private double lat;
    private double lng;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rumah_sehat);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setInit();
        setAuthInstance();
        setDatabaseInstance();
        setSharedPreferences();
    }

    private void setInit(){
        frmNamaKK = (EditText)findViewById(R.id.namaKK);
        frmAlamat = (EditText)findViewById(R.id.alamatKK);
        frmJmlAnggota = (EditText)findViewById(R.id.jmlAnggota);
        frmNoRumah = (EditText)findViewById(R.id.noRumah);
        spinnerRT = (Spinner)findViewById(R.id.spinner_rt);
        spinnerRW = (Spinner)findViewById(R.id.spinner_rw);
        rg22 = (RadioGroup)findViewById(R.id.rg22);
        rg21 = (RadioGroup)findViewById(R.id.rg21);
        rg20 = (RadioGroup)findViewById(R.id.rg20);
        rg19 = (RadioGroup)findViewById(R.id.rg19);
        sdf = new  SimpleDateFormat("dd/MM/yyyy h:mm:ss a");
        for(int i = 1; i<=17; i++)
        {
            int rID = getResources().getIdentifier("rg" + i, "id", this.getBaseContext().getPackageName());
            rg[i-1] = (RadioGroup)findViewById(rID);
        }
        btnSend = (Button)findViewById(R.id.send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmit();
            }
        });
        frmAlamat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAutocompleteActivity();
            }
        });
    }
    private void setSharedPreferences() {
        pref = getApplicationContext().getSharedPreferences(SessionString.EXTRA_DATABASE_SESSION, MODE_PRIVATE);
        editor = pref.edit();
    }

    private void openAutocompleteActivity() {
        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .build(this);
            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
        } catch (GooglePlayServicesRepairableException e) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, e.getConnectionStatusCode(),
                    0 /* requestCode */).show();
        } catch (GooglePlayServicesNotAvailableException e) {
            String message = "Google Play Services is not available: " +
                    GoogleApiAvailability.getInstance().getErrorString(e.errorCode);

            Log.e("ERROR", message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.i("TAG", "Place Selected: " + place.getName());
                String addressPlace = String.valueOf(place.getAddress());
                frmAlamat.setText(addressPlace.trim());
                LatLng latLng = place.getLatLng();
                lat = latLng.latitude;
                lng = latLng.longitude;
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.e("TAG", "Error: Status = " + status.toString());
            }
        }
    }

    private void onSubmit(){
        if(!frmJmlAnggota.getText().toString().trim().equals("") && !frmNoRumah.getText().toString().trim().equals("")
                && !frmAlamat.getText().toString().trim().equals("") && rg19.getCheckedRadioButtonId() != -1
                && rg20.getCheckedRadioButtonId() != -1 && rg21.getCheckedRadioButtonId() != -1
                && rg22.getCheckedRadioButtonId() != -1){
            String txtJmlAnggota = frmJmlAnggota.getText().toString().trim();
            String txtNoRumah = frmNoRumah.getText().toString().trim();
            String txtRT = spinnerRT.getSelectedItem().toString().trim();
            String txtRW = spinnerRW.getSelectedItem().toString().trim();
            String idPetugas = pref.getString(SessionString.EXTRA_KEY_ID_USER, null);
            String waktu = sdf.format(Calendar.getInstance().getTime().getTime());
            String koordinat = String.valueOf(lat) + ", " + String.valueOf(lng);
            String alamat = frmAlamat.getText().toString().trim();

            String jamban = ((RadioButton)findViewById(rg21.getCheckedRadioButtonId())).getText().toString();
            String spal = ((RadioButton)findViewById(rg22.getCheckedRadioButtonId())).getText().toString();
            String pjb = ((RadioButton)findViewById(rg20.getCheckedRadioButtonId())).getText().toString();
            String sampah = ((RadioButton)findViewById(rg19.getCheckedRadioButtonId())).getText().toString();

            String idSAB = mDatabase.child("sab").push().getKey();


            int totalNilai = 0;
            boolean hasValue = true;
            String status;
            String[] txtRB = new String[17];
            String[] nilaiRB = new String[17];
            HashMap<String, String> nilaiRS = new HashMap<>();

            for(int i = 0; i<=16; i++)
            {
                if(rg[i].getCheckedRadioButtonId() != -1 && rg19.getCheckedRadioButtonId() != -1
                        && rg20.getCheckedRadioButtonId() != -1 && rg21.getCheckedRadioButtonId() != -1){
                    if(i < 8){
                        txtRB[i] = ((RadioButton)findViewById(rg[i].getCheckedRadioButtonId())).getText().toString().trim().substring(0, 1);
                        nilaiRB[i] = String.valueOf(setNilai(txtRB[i]) * 31);
                    }else if(i >= 8 && i < 12){
                        txtRB[i] = ((RadioButton)findViewById(rg[i].getCheckedRadioButtonId())).getText().toString().trim().substring(0, 1);
                        nilaiRB[i] = String.valueOf(setNilai(txtRB[i]) * 25);
                    }else if(i >= 12 && i <= 16){
                        txtRB[i] = ((RadioButton)findViewById(rg[i].getCheckedRadioButtonId())).getText().toString().trim().substring(0, 1);
                        nilaiRB[i] = String.valueOf(setNilai(txtRB[i]) * 44);
                    }

                    nilaiRS.put("nilai_" + i , nilaiRB[i]);
                    totalNilai = totalNilai + Integer.valueOf(nilaiRB[i]);
                }else{
                    hasValue = false;
                }
            }
            if(hasValue){
                if(frmNamaKK.getText().toString().contains(",")){
                    String[] splitNamaKK = frmNamaKK.getText().toString().trim().split(",");
                    for(int i = 0;i<splitNamaKK.length;i++){
                        KK setKK = new KK(alamat, splitNamaKK[i], txtJmlAnggota, txtNoRumah, idPetugas);
                        String pushKK = mDatabase.child("kk").push().getKey();
                        mDatabase.child("kk").child(pushKK).setValue(setKK);
                        if(totalNilai <= 1068){
                            status = "Rumah Tidak Sehat";
                        }else {
                            status = "Rumah Sehat";
                        }

                        submitRS(pushKK, koordinat, waktu, idPetugas, totalNilai, status, jamban, spal, pjb, sampah, idSAB, txtRT, txtRW, nilaiRS);
                    }

                }else{
                    String txtNamaKK = frmNamaKK.getText().toString().trim();
                    KK setKK = new KK(alamat, txtNamaKK, txtJmlAnggota, txtNoRumah, idPetugas);
                    String pushKK = mDatabase.child("kk").push().getKey();
                    mDatabase.child("kk").child(pushKK).setValue(setKK);
                    if(totalNilai <= 1068){
                        status = "Rumah Tidak Sehat";
                    }else {
                        status = "Rumah Sehat";
                    }

                    submitRS(pushKK, koordinat, waktu, idPetugas, totalNilai, status, jamban, spal, pjb, sampah, idSAB, txtRT, txtRW, nilaiRS);
                }


            }else{
                Toast.makeText(this, "Error harap check semua opsi!", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Toast.makeText(this, "Error harap isi semua opsi!", Toast.LENGTH_SHORT).show();
        }


    }

    private void submitRS(String pushKK, String koordinat, String waktu, String idPetugas,
                          int totalNilai, String status, String jamban, String spal, String pjb, String sampah,
                          String idSAB, String txtRT, String txtRW, HashMap<String, String> nilaiRS){
        RS setRS = new RS(pushKK, koordinat, waktu, idPetugas, totalNilai, status, jamban, spal, pjb, sampah, idSAB, txtRT, txtRW);

        String pushRS = mDatabase.child("rs").push().getKey();
        mDatabase.child("rs").child(pushRS).child("data").setValue(setRS);
        mDatabase.child("rs").child(pushRS).child("nilai").setValue(nilaiRS);

        Toast.makeText(this, "Data berhasil dikirim!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getBaseContext(), jenis_sab.class);
        intent.putExtra("idKK", pushKK);
        intent.putExtra("idSAB", idSAB);
        intent.putExtra("idRS", pushRS);
        intent.putExtra("koordinat", koordinat);
        intent.putExtra("alamat", frmAlamat.getText().toString().trim());
        startActivity(intent);
        finish();
    }

    private int setNilai(String abc){
        int nilaiBobot = 0;
        switch (abc) {
            case "a":
                nilaiBobot = 0;
                break;
            case "b":
                nilaiBobot = 1;
                break;
            case "c":
                nilaiBobot = 2;
                break;
            case "d":
                nilaiBobot = 3;
                break;
            case "e":
                nilaiBobot = 4;
                break;
        }
        return nilaiBobot;
    }
    private void setAuthInstance() {
        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

            }
        };
    }

    private void setDatabaseInstance() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // todo: goto back activity from here
                RSActivity.this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
