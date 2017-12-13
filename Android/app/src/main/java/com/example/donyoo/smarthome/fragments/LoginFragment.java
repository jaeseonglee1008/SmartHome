package com.example.donyoo.smarthome.fragments;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.donyoo.smarthome.DisplayMessageActivity;
import com.example.donyoo.smarthome.model.Login;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.example.donyoo.smarthome.ProfileActivity;
import com.example.donyoo.smarthome.R;
import com.example.donyoo.smarthome.model.Response;
import com.example.donyoo.smarthome.network.NetworkUtil;
import com.example.donyoo.smarthome.utils.Constants;

import java.io.IOException;

import retrofit2.adapter.rxjava.HttpException;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static com.example.donyoo.smarthome.utils.Validation.validateEmail;
import static com.example.donyoo.smarthome.utils.Validation.validateFields;
import static com.example.donyoo.smarthome.utils.Validation.validateEmail;
import static com.example.donyoo.smarthome.utils.Validation.validateFields;

public class LoginFragment extends Fragment {

    public static final String TAG = LoginFragment.class.getSimpleName();
    private EditText mEtEmail;
    private EditText mEtPassword;
    private Button mBtLogin;
    private TextView mTvRegister;
    private TextView mTvForgotPassword;
    private TextInputLayout mTiEmail;
    private TextInputLayout mTiPassword;
    private ProgressBar mProgressBar;

    private CompositeSubscription mSubscriptions;
    private SharedPreferences mSharedPreferences;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login,container,false);
        mSubscriptions = new CompositeSubscription();
        initViews(view);
        initSharedPreferences();
        return view;
    }

    private void initViews(View v) {
        mEtEmail = (EditText) v.findViewById(R.id.et_email);
        mEtPassword = (EditText) v.findViewById(R.id.et_password);
        mBtLogin = (Button) v.findViewById(R.id.btn_login);
        mTiEmail = (TextInputLayout) v.findViewById(R.id.ti_email);
        mTiPassword = (TextInputLayout) v.findViewById(R.id.ti_password);
        mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
        mTvRegister = (TextView) v.findViewById(R.id.tv_register);
        mTvForgotPassword = (TextView) v.findViewById(R.id.tv_forgot_password);

        mBtLogin.setOnClickListener(view -> login());
        mTvRegister.setOnClickListener(view -> goToRegister());
        mTvForgotPassword.setOnClickListener(view -> showDialog());
    }

    private void initSharedPreferences() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    private void login() {
        setError();
        String email = mEtEmail.getText().toString();
        String password = mEtPassword.getText().toString();

        // 12/5/17 add user json.
        Login login = new Login(email, password);
        int err = 0;
        if (!validateEmail(email)) {
            err++;
            mTiEmail.setError("Email should be valid !");
        }

        if (!validateFields(password)) {
            err++;
            mTiPassword.setError("Password should not be empty !");
        }

        if (err == 0) {
            loginProcess(login);
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            showSnackBarMessage("Enter Valid Details !");
        }
    }

    private void setError() {
        mTiEmail.setError(null);
        mTiPassword.setError(null);
    }

    private void loginProcess(Login login) {
        mSubscriptions.add(NetworkUtil.getRetrofit().login(login)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponse,this::handleError));
    }

    // this is handler after loginProcess get the response from the server.
    private void handleResponse(Response response) {
        mProgressBar.setVisibility(View.GONE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        Log.d("token", response.getToken());
        editor.putString(Constants.TOKEN,response.getToken());
        editor.putString(Constants.EMAIL,response.getMessage());
        editor.apply();

        mEtEmail.setText(null);
        mEtPassword.setText(null);

        // bring it to profile page in android
        Intent intent = new Intent(getActivity(), ProfileActivity.class);
        startActivity(intent);
    }

    // if any error occurs between
    private void handleError(Throwable error) {
        mProgressBar.setVisibility(View.GONE);

        if (error instanceof HttpException) {
            Gson gson = new GsonBuilder().create();
            try {
                String errorBody = ((HttpException) error).response().errorBody().string();
                Response response = gson.fromJson(errorBody,Response.class);
                showSnackBarMessage(response.getMessage());

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showSnackBarMessage("Network Error !");
        }
    }

    private void showSnackBarMessage(String message) {
        if (getView() != null) {
            Snackbar.make(getView(),message, Snackbar.LENGTH_SHORT).show();
        }
    }

    // if register button is clicked
    private void goToRegister(){
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        RegisterFragment fragment = new RegisterFragment();
        ft.replace(R.id.fragmentFrame,fragment,RegisterFragment.TAG);
        ft.commit();
    }

    private void showDialog(){
        ResetPasswordDialog fragment = new ResetPasswordDialog();
        fragment.show(getFragmentManager(), ResetPasswordDialog.TAG);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscriptions.unsubscribe();
    }
}
