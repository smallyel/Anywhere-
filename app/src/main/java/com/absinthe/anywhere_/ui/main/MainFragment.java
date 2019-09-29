package com.absinthe.anywhere_.ui.main;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.absinthe.anywhere_.AnywhereApplication;
import com.absinthe.anywhere_.R;
import com.absinthe.anywhere_.adapter.SelectableCardsAdapter;
import com.absinthe.anywhere_.model.AnywhereEntity;
import com.absinthe.anywhere_.services.CollectorService;
import com.absinthe.anywhere_.ui.settings.SettingsActivity;
import com.absinthe.anywhere_.utils.ConstUtil;
import com.absinthe.anywhere_.utils.EditUtils;
import com.absinthe.anywhere_.utils.ImageUtils;
import com.absinthe.anywhere_.utils.PermissionUtil;
import com.absinthe.anywhere_.utils.SPUtils;
import com.absinthe.anywhere_.utils.TextUtils;
import com.absinthe.anywhere_.viewmodel.AnywhereViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class MainFragment extends Fragment implements LifecycleOwner {
    private static final String TAG = "MainFragment";
    private static final int REQUEST_CODE_ACTION_MANAGE_OVERLAY_PERMISSION = 1001;
    private Context mContext;
    private String workingMode;

    private static AnywhereViewModel mViewModel;
    private SelectableCardsAdapter adapter;

    static MainFragment newInstance() {
        return new MainFragment();
    }
    public static AnywhereViewModel getViewModelInstance() {
        return mViewModel;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mContext = getActivity();
        View view = inflater.inflate(R.layout.main_fragment, container, false);
        initView(view);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initObserver();
        PermissionUtil.checkShizukuOnWorking(mContext);

        boolean isFirstLaunch = SPUtils.getBoolean(mContext, ConstUtil.SP_KEY_FIRST_LAUNCH);
        if (isFirstLaunch) {
            new MaterialTapTargetPrompt.Builder(this)
                    .setTarget(R.id.fab)
                    .setPrimaryText("创建你的第一个 Anywhere- 吧！")
                    .setBackgroundColour(getResources().getColor(R.color.colorAccent))
                    .setPromptStateChangeListener((prompt, state) -> {
                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED)
                        {
                            Toast.makeText(mContext, getString(R.string.toast_open_pop_up_when_background_permission), Toast.LENGTH_LONG).show();
                            if (PermissionUtil.isMIUI()) {
                                PermissionUtil.goToMIUIPermissionManager(mContext);
                            }
                        }
                    })
                    .show();
            SPUtils.putBoolean(mContext, ConstUtil.SP_KEY_FIRST_LAUNCH, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle bundle = getArguments();
        if (bundle != null) {
            String packageName = bundle.getString("packageName");
            String className = bundle.getString("className");
            int classNameType = bundle.getInt("classNameType");

            String appName;
            if (packageName != null && className != null) {
                appName = TextUtils.getAppName(mContext, packageName);

                Log.d(TAG, "onResume:" + packageName + "," + className);
                EditUtils.editAnywhere((Activity) mContext, packageName, className, classNameType, appName);

                bundle.clear();
            }
        }

    }

    private void checkWorkingPermission() {
        Log.d(TAG, "workingMode = " + workingMode);
        if (workingMode != null) {
            if (workingMode.isEmpty()) {
                final int[] selected = {-1};
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.settings_working_mode)
                        .setSingleChoiceItems(new CharSequence[]{"Root", "Shizuku"}, 0, (dialogInterface, i) -> selected[0] = i)
                        .setPositiveButton(R.string.dialog_delete_positive_button, (dialogInterface, i) -> {
                            switch (selected[0]) {
                                case 0:
                                    mViewModel.getWorkingMode().setValue(ConstUtil.WORKING_MODE_ROOT);
                                    break;
                                case 1:
                                    mViewModel.getWorkingMode().setValue(ConstUtil.WORKING_MODE_SHIZUKU);
                                    break;
                                default:
                                    Log.d(TAG, "default");
                            }
                        })
                        .setNegativeButton(R.string.dialog_delete_negative_button, null)
                        .show();
            } else {
                if (workingMode.equals(ConstUtil.WORKING_MODE_SHIZUKU)) {
                    if (PermissionUtil.checkShizukuOnWorking(mContext) && PermissionUtil.shizukuPermissionCheck(getActivity())) {
                        startCollector();
                    }
                } else if (workingMode.equals(ConstUtil.WORKING_MODE_ROOT)) {
                    if (PermissionUtil.upgradeRootPermission(mContext.getPackageCodePath())) {
                        startCollector();
                    } else {
                        Log.d(TAG, "ROOT permission denied.");
                        Toast.makeText(mContext, getString(R.string.toast_root_permission_denied), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

    }

    private void startCollector() {
        Intent intent = new Intent(mContext, CollectorService.class);
        intent.putExtra(CollectorService.COMMAND, CollectorService.COMMAND_OPEN);
        Toast.makeText(getContext(), R.string.toast_collector_opened, Toast.LENGTH_SHORT).show();

        mContext.startService(intent);
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(homeIntent);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ACTION_MANAGE_OVERLAY_PERMISSION && Settings.canDrawOverlays(mContext)) {
            checkWorkingPermission();
        }
    }

    private void setUpRecyclerView(RecyclerView recyclerView) {
        List<AnywhereEntity> anywhereEntityList = new ArrayList<>();

        adapter = new SelectableCardsAdapter(mContext);
        adapter.setItems(anywhereEntityList);
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.bottom_bar_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.toolbar_settings) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }



    private void initView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        setUpRecyclerView(recyclerView);
        setHasOptionsMenu(true);

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(clickView -> {
            if (PermissionUtil.checkOverlayPermission(getActivity(), REQUEST_CODE_ACTION_MANAGE_OVERLAY_PERMISSION)) {
                checkWorkingPermission();
            }
        });

        String backgroundUri = SPUtils.getString(mContext, ConstUtil.SP_KEY_CHANGE_BACKGROUND);

        if (!backgroundUri.isEmpty()) {
            Log.d(TAG, "backgroundUri = " + backgroundUri);
            mViewModel.getBackground().setValue(backgroundUri);
        }
    }

    private void initObserver() {
        mViewModel = ViewModelProviders.of(this).get(AnywhereViewModel.class);
        mViewModel.getWorkingMode().setValue(AnywhereApplication.workingMode);

        final Observer<String> commandObserver = s -> {
            if (workingMode.equals(ConstUtil.WORKING_MODE_SHIZUKU)) {
                if (PermissionUtil.shizukuPermissionCheck(getActivity())) {
                    PermissionUtil.execShizukuCmd(s);
                }
            } else if (workingMode.equals(ConstUtil.WORKING_MODE_ROOT)) {
                if (PermissionUtil.upgradeRootPermission(mContext.getPackageCodePath())) {
                    PermissionUtil.execRootCmd(s);
                }
            }
        };
        mViewModel.getCommand().observe(this, commandObserver);
        mViewModel.getAllAnywhereEntities().observe(this, anywhereEntities -> adapter.setItems(anywhereEntities));
        mViewModel.getWorkingMode().observe(this, s -> {
            AnywhereApplication.workingMode = workingMode = s;
            SPUtils.putString(mContext, ConstUtil.SP_KEY_WORKING_MODE, s);
        });

        final Observer<String> backgroundObserver = s -> {
            ImageView ivBackground = Objects.requireNonNull(getActivity()).findViewById(R.id.iv_background);
            if (s.isEmpty()) {
                ivBackground.setBackground(null);
            } else {
                ImageUtils.loadBackgroundPic(mContext, ivBackground);
                ImageUtils.setActionBarTransparent(getActivity());
            }
            SPUtils.putString(mContext, ConstUtil.SP_KEY_CHANGE_BACKGROUND, s);
        };
        mViewModel.getBackground().observe(this, backgroundObserver);
    }
}
