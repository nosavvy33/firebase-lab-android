package pe.mistreating.undying.firebaseapp2;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.List;

import pe.mistreating.undying.firebaseapp2.adapters.PostRVAdapter;
import pe.mistreating.undying.firebaseapp2.classes.Post;
import pe.mistreating.undying.firebaseapp2.classes.User;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private FirebaseAnalytics mFirebaseAnalytics;
    private RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtenemos el refreshedToken (instanceid)
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // Nos suscribimos al tópico 'ALL'
        FirebaseMessaging.getInstance().subscribeToTopic("ALL");


        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Bundle bundle = new Bundle();
        bundle.putString("fullname", "Danilo Lopez");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);

        mFirebaseAnalytics.setUserProperty("username", "dlopez");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "user: " + user);
        // Get currentuser from FirebaseAuth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "currentUser: " + currentUser);

        // Save/Update current user to Firebase Database
        User userl = new User();
        userl.setUid(currentUser.getUid());
        userl.setDisplayName(currentUser.getDisplayName());
        userl.setEmail(currentUser.getEmail());
        userl.setPhotoUrl((currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null));
        // user.setEtc...
// Obteniendo datos del usuario de Firebase en tiempo real
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange " + dataSnapshot.getKey());

                // Obteniendo datos del usuario
                User user = dataSnapshot.getValue(User.class);
                setTitle(user.getDisplayName());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "onCancelled " + databaseError.getMessage(), databaseError.toException());
            }
        });

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.child(userl.getUid()).setValue(userl)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onSuccess");
                        } else {
                            Log.e(TAG, "onFailure", task.getException());
                        }
                    }
                });


        // Lista de post con RecyclerView
        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final PostRVAdapter postRVAdapter = new PostRVAdapter();
        recyclerView.setAdapter(postRVAdapter);

        // Obteniendo lista de posts de Firebase (con realtime)
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded " + dataSnapshot.getKey());

                // Obteniendo nuevo post de Firebase
                String postKey = dataSnapshot.getKey();
                final Post addedPost = dataSnapshot.getValue(Post.class);
                Log.d(TAG, "addedPost " + addedPost);

                // Actualizando adapter datasource
                List<Post> posts = postRVAdapter.getPosts();
                posts.add(0, addedPost);
                postRVAdapter.notifyDataSetChanged();

                // ...
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged " + dataSnapshot.getKey());

                // Obteniendo post modificado de Firebase
                String postKey = dataSnapshot.getKey();
                Post changedPost = dataSnapshot.getValue(Post.class);
                Log.d(TAG, "changedPost " + changedPost);

                // Actualizando adapter datasource
                List<Post> posts = postRVAdapter.getPosts();
                int index = posts.indexOf(changedPost); // Necesario implementar Post.equals()
                if (index != -1) {
                    posts.set(index, changedPost);
                }
                postRVAdapter.notifyDataSetChanged();

                // ...
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved " + dataSnapshot.getKey());

                // Obteniendo post eliminado de Firebase
                String postKey = dataSnapshot.getKey();
                Post removedPost = dataSnapshot.getValue(Post.class);
                Log.d(TAG, "removedPost " + removedPost);

                // Actualizando adapter datasource
                List<Post> posts = postRVAdapter.getPosts();
                posts.remove(removedPost); // Necesario implementar Post.equals()
                postRVAdapter.notifyDataSetChanged();

                // ...


            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved " + dataSnapshot.getKey());

                // A post has changed position, use the key to determine if we are
                // displaying this post and if so move it.
                Post movedPost = dataSnapshot.getValue(Post.class);
                String postKey = dataSnapshot.getKey();

                // ...
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "onCancelled " + databaseError.getMessage(), databaseError.toException());
            }
        };
        postsRef.addChildEventListener(childEventListener);

        // Cargar parámetros de Remote Config
        final FirebaseRemoteConfig mRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mRemoteConfig.setConfigSettings(configSettings);

        // Definimos los valores predeterminados en caso no se obtenga de Remote Config
        HashMap<String, Object> defaults = new HashMap<>();
        defaults.put("tecsup_day_enabled", false);
        defaults.put("tecsup_day_message", "");
        defaults.put("tecsup_day_primarycolor", "#3F51B5");
        mRemoteConfig.setDefaults(defaults);

        // Tiempo de vida de la caché en segundos
        long cacheExpiration = 3600;

        // En modo desarrollo deshabilitamos la caché
        if (mRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        // Obtenemos los parámetros del Remote Config
        mRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onSuccess");
                            // Activamos los parámetros de config
                            mRemoteConfig.activateFetched();

                            // Aplicamos cambios en las vistas
                            boolean tecsup_day_enabled = mRemoteConfig.getBoolean("tecsup_day_enabled");
                            if(tecsup_day_enabled){
                                String tecsup_day_message = mRemoteConfig.getString("tecsup_day_message");
                                new AlertDialog.Builder(MainActivity.this).setMessage(tecsup_day_message).create().show();

                                String tecsup_day_primarycolor = mRemoteConfig.getString("tecsup_day_primarycolor");
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(tecsup_day_primarycolor)));
                            }

                        } else {
                            Log.e(TAG, "onFailure", task.getException());
                        }
                    }
                });
// Forzar un error
        getIntent().getExtras().getString("NO_EXISTE").getBytes();

    }



    private static final int REGISTER_FORM_REQUEST = 100;

    public void showRegister(View view){
        startActivityForResult(new Intent(this, RegisterActivity.class), REGISTER_FORM_REQUEST);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                callLogout(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void callLogout(View view){
        Log.d(TAG, "Ssign out user");
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(MainActivity.this,LoginActivity.class));
        }


}

