package com.alkantemirov.openlibrary;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ServerUnavailableFragment extends Fragment {
    public ServerUnavailableFragment() {}
    public static ServerUnavailableFragment newInstance() {
        ServerUnavailableFragment fragment = new ServerUnavailableFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_server_unavailable, container, false);
    }
}