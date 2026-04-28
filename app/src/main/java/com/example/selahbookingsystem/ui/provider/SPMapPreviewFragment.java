package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class SPMapPreviewFragment extends Fragment {

    private static final String ARG_LAT = "arg_lat";
    private static final String ARG_LNG = "arg_lng";

    private GoogleMap map;
    private double lat;
    private double lng;

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

    @Nullable
    @Override
    public View onCreateView(
            @NonNull android.view.LayoutInflater inflater,
            @Nullable android.view.ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        FrameLayout root = new FrameLayout(requireContext());
        root.setId(View.generateViewId());
        root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            lat = getArguments().getDouble(ARG_LAT, 0);
            lng = getArguments().getDouble(ARG_LNG, 0);
        }

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();

        getChildFragmentManager()
                .beginTransaction()
                .replace(view.getId(), mapFragment)
                .commitNowAllowingStateLoss();

        mapFragment.getMapAsync(gMap -> {
            map = gMap;

            map.getUiSettings().setAllGesturesEnabled(false);
            map.getUiSettings().setMapToolbarEnabled(false);
            map.getUiSettings().setCompassEnabled(false);

            updateLocation(lat, lng);
        });
    }

    public void updateLocation(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;

        if (map == null) return;
        if (lat == 0 || lng == 0) return;

        LatLng pos = new LatLng(lat, lng);

        map.clear();
        map.addMarker(new MarkerOptions().position(pos));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
    }
}