package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class SPMapPreviewFragment extends Fragment {

    private static final String ARG_LAT = "arg_lat";
    private static final String ARG_LNG = "arg_lng";

    private GoogleMap map;

    public static SPMapPreviewFragment newInstance(double lat, double lng) {
        SPMapPreviewFragment f = new SPMapPreviewFragment();
        Bundle b = new Bundle();
        b.putDouble(ARG_LAT, lat);
        b.putDouble(ARG_LNG, lng);
        f.setArguments(b);
        return f;
    }

    public SPMapPreviewFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Put a SupportMapFragment inside this fragment
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager()
                .beginTransaction()
                .replace(getId(), mapFragment) // note: we’ll attach this fragment to a container ID
                .commitNowAllowingStateLoss();
    }

    public void renderIntoContainer(int containerId) {
        // This helper method is used from the activity
    }

    public void updateLocation(double lat, double lng) {
        if (map == null) return;
        LatLng pos = new LatLng(lat, lng);
        map.clear();
        map.addMarker(new MarkerOptions().position(pos));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f));
    }

    public OnMapReadyCallback callbackFor(double lat, double lng) {
        return gMap -> {
            map = gMap;

            // Disable interactions so it feels like a “preview frame”
            map.getUiSettings().setAllGesturesEnabled(false);
            map.getUiSettings().setMapToolbarEnabled(false);
            map.getUiSettings().setCompassEnabled(false);

            updateLocation(lat, lng);
        };
    }
}

