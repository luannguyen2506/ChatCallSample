package com.stringee.kit.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.stringee.apptoappcallsample.R;
import com.stringee.kit.ui.adapter.FileAdapter;
import com.stringee.kit.ui.commons.CallBack;
import com.stringee.kit.ui.commons.DataHandler;
import com.stringee.kit.ui.commons.utils.FileUtils;
import com.stringee.kit.ui.commons.utils.Utils;
import com.stringee.kit.ui.model.StringeeFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectFileActivity extends MActivity implements CallBack {
    private List<StringeeFile> files = new ArrayList<StringeeFile>();
    private ListView lvFile;
    private FileAdapter fileAdapter;
    int count = 0;
    String pathCurrentFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_file);
        initActionbar();
        setComponentView();
        setListenerView();
        initData();
    }

    @Override
    public void onBackPressed() {
        if (count > 1) {
            File file = new File(pathCurrentFile);
            getFiles(file.getParent());
            count--;
        } else if (count == 1) {
            initData();
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void initActionbar() {
        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setTitle(R.string.select_file);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);
    }

    private void setComponentView() {
        lvFile = (ListView) findViewById(R.id.lv_file);
    }

    private void setListenerView() {
        lvFile.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                StringeeFile file = (StringeeFile) fileAdapter.getItem(position);
                // check directory
                if (file.getType() == StringeeFile.TYPE_BACK) {
                    onBackPressed();
                } else if (file.getType() == StringeeFile.TYPE_DIRECTORY) {
                    getFiles(file.getPath());
                    count++;
                } else {
                    Intent intent = getIntent();
                    intent.putExtra("path", file.getPath());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
    }

    private void initData() {
        count = 0;
        getSupportActionBar().setTitle(R.string.select_file);

        files = new ArrayList<StringeeFile>();
        String storagePath = FileUtils.getStorage(getBaseContext());
        StringeeFile storageFile = new StringeeFile();
        storageFile.setPath(storagePath);
        storageFile.setName(getString(R.string.stringee_external_storage));
        storageFile.setType(StringeeFile.TYPE_DIRECTORY);
        files.add(storageFile);

        String stPath = Utils.getAppDirectory(getBaseContext()).getAbsolutePath();
        StringeeFile stFile = new StringeeFile();
        stFile.setPath(stPath);
        stFile.setName(getString(R.string.app_name));
        stFile.setType(StringeeFile.TYPE_DIRECTORY);
        files.add(stFile);

        if (files == null)
            return;
        if (fileAdapter == null) {
            fileAdapter = new FileAdapter(getBaseContext(), files);
            lvFile.setAdapter(fileAdapter);
        } else
            fileAdapter.setFiles(files);
    }

    private void getFiles(String folder) {
        pathCurrentFile = folder;
        showProgress(getString(R.string.loading));
        Object[] params = new Object[2];
        params[1] = folder;
        DataHandler dataHandler = new DataHandler(this, this);
        dataHandler.execute(params);
    }

    @Override
    public void start() {
    }

    @Override
    public void doWork(Object... params) {
        String folder = (String) params[1];
        files = new ArrayList<StringeeFile>();
        StringeeFile StringeeFile = new StringeeFile();
        StringeeFile.setType(StringeeFile.TYPE_BACK);
        StringeeFile.setName("...");
        files.add(StringeeFile);
        List<StringeeFile> fs = FileUtils.getFiles(folder, null, 1);
        if (fs != null)
            if (fs.size() > 0)
                files.addAll(fs);
    }

    @Override
    public void end(Object[] params) {
        dismissProgress();
        if (files == null)
            return;
        if (files.size() == 0)
            return;
        if (fileAdapter == null) {
            fileAdapter = new FileAdapter(getBaseContext(), files);
            lvFile.setAdapter(fileAdapter);
        } else
            fileAdapter.setFiles(files);
        getSupportActionBar().setTitle(pathCurrentFile);
    }
}
