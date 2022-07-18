package com.namthan.myfilemanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.namthan.myfilemanager.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private ActivityMainBinding binding = null;
    private List<FileModel> fileModelList;
    String rootPath;
    List<String> pathHistory = new ArrayList<>();
    private File file;
    FileAdapter adapter;
    FileModel actionFile;
    String action;

    public final String[] EXTERNAL_PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    public final int EXTERNAL_REQUEST = 138;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());

        requestForExternalStoragePermission();

        rootPath = Environment.getExternalStorageDirectory().getPath();
        file = new File(rootPath);
        List<File> files = Utils.getListFiles(file);
        if (files != null) {
            fileModelList = files.stream().map(Utils::fromFileToFileModel).collect(Collectors.toList());
        }

        adapter = new FileAdapter(fileModelList, this);
        binding.fileRcv.setAdapter(adapter);

        registerForContextMenu(binding.fileRcv);
    }

    public boolean canAccessExternalSd() {
        return (hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    private boolean hasPermission(String permission) {
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, permission));

    }

    public boolean requestForExternalStoragePermission() {

        boolean isPermissionOn = true;
        final int version = Build.VERSION.SDK_INT;
        if (version >= 23) {
            if (!canAccessExternalSd()) {
                isPermissionOn = false;
                requestPermissions(EXTERNAL_PERMS, EXTERNAL_REQUEST);
            }
        }

        return isPermissionOn;
    }

    @Override
    public void onItemClick(FileModel fileModel) {
        if (fileModel.getType() == com.namthan.myfilemanager.FileType.DIRECTORY) {
            pathHistory.add(rootPath);
            rootPath = rootPath + "/" + fileModel.getFileName();
            setlistFile(rootPath);
        } else {
            openFile(fileModel);
        }
    }

    public void setlistFile(String path) {
        file = new File(path);
        List<File> files = Utils.getListFiles(file);
        if (files != null) {
            fileModelList = files.stream().map(Utils::fromFileToFileModel).collect(Collectors.toList());
        }
        adapter.setlistFile(fileModelList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (pathHistory.size() <= 0) {
            super.onBackPressed();
            return;
        }
        int lastIndex = pathHistory.size() - 1;
        rootPath = pathHistory.get(lastIndex);
        pathHistory.remove(lastIndex);
        setlistFile(rootPath);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int position = adapter.getPosition();
        FileModel fileModel = adapter.getFile(position);
        if (item.getTitle() == "Đổi tên") {
            StringBuilder newName = new StringBuilder(fileModel.getFileName());
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Đổi tên");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(fileModel.getFileName());
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                newName.append(input.getText().toString());
                renameFile(fileModel, newName.toString());
                setlistFile(rootPath);
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        }

        if (item.getTitle() == "Xoá") {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Xoá");
            builder.setPositiveButton("OK", (dialog, which) -> {
                File toDelete = new File(fileModel.getPath());
                toDelete.delete();
                setlistFile(rootPath);
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        }

        if (item.getTitle() == "Sao chép" || item.getTitle() == "Di chuyển") {
            actionFile = fileModel;
            action = item.getTitle().toString();
            return true;
        }

        return super.onContextItemSelected(item);
    }

    public void renameFile(FileModel fileModel, String newName) {
        File oldFile = new File(fileModel.getDirectoryPath(), fileModel.getFileName());
        File newFile;

        if (fileModel.getType() == FileType.DIRECTORY) {
            newFile = new File(fileModel.getDirectoryPath(), newName);
        } else {
            newFile = new File(fileModel.getDirectoryPath(), newName + "." + fileModel.getExtension());
        }

        oldFile.renameTo(newFile);
        setlistFile(rootPath);
    }

    public void copyFile(FileModel source, String target) {
        try {
            FileInputStream inStream = new FileInputStream(source.getPath());
            FileOutputStream outStream = new FileOutputStream(target);
            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setlistFile(rootPath);
    }

    public void moveFile(FileModel source, String target) {
        File fromFile = new File(source.getPath());
        File toFile = new File(target);
        fromFile.renameTo(toFile);
        setlistFile(rootPath);
    }

    public void makeDirectory(String target) {
        File directory = new File(target);
        directory.mkdirs();
        setlistFile(rootPath);
    }

    public void createTextFile(String name, String body) {
        try {
            File txtFile = new File(name);
            FileWriter writer = new FileWriter(txtFile);
            writer.append(body);
            writer.flush();
            writer.close();
            setlistFile(rootPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openFile(FileModel fileModel) {
        if (fileModel.getType() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);

        if (fileModel.getType() == FileType.IMAGE) {
            intent.setDataAndType(
                    FileProvider.getUriForFile(getApplicationContext(),
                            BuildConfig.APPLICATION_ID + ".provider",
                            new File(fileModel.getPath())), "image/*"
            );
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        if (fileModel.getType() == FileType.TEXT) {
            intent.setDataAndType(
                    FileProvider.getUriForFile(getApplicationContext(),
                            BuildConfig.APPLICATION_ID + ".provider",
                            new File(fileModel.getPath())), "text/plain"
            );
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivity(Intent.createChooser(intent, "Select chooser"));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.create_directory) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Tạo thư mục");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                makeDirectory(rootPath + "/" + input.getText().toString());
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
            return true;
        }
        if (item.getItemId() == R.id.paste) {
            if (actionFile != null) {
                if (action.equals("Sao chép")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Bạn có chắc muốn sao chép đến đây?");
                    builder.setPositiveButton("OK", (dialog, which) -> {
                        copyFile(actionFile, rootPath + "/" + actionFile.getFileName());
                        actionFile = null;
                        action = null;
                    });
                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                    builder.show();
                } else if (action.equals("Di chuyển")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Bạn có chắc muốn di chuyển đến đây?");
                    builder.setPositiveButton("OK", (dialog, which) -> {
                        moveFile(actionFile, rootPath + "/" + actionFile.getFileName());
                        actionFile = null;
                        action = null;
                    });
                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                    builder.show();
                }
            }
            return true;
        }
        if (item.getItemId() == R.id.create_file) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Tạo file");
            TextView inputNameLabel = new TextView(this);
            inputNameLabel.setText("Tên file");
            final EditText inputName = new EditText(this);
            TextView inputBodyLabel = new TextView(this);
            inputBodyLabel.setText("Nội dung");
            final EditText inputBody = new EditText(this);
            inputName.setInputType(InputType.TYPE_CLASS_TEXT);
            inputBody.setInputType(InputType.TYPE_CLASS_TEXT);
            final LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(inputNameLabel);
            layout.addView(inputName);
            layout.addView(inputBodyLabel);
            layout.addView(inputBody);
            builder.setView(layout);
            builder.setPositiveButton("OK", (dialog, which) -> {
                createTextFile(rootPath + "/" + inputName.getText().toString() + ".txt", inputBody.getText().toString());
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}