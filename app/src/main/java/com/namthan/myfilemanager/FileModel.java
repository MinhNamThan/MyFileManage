package com.namthan.myfilemanager;

public class FileModel {
    private String fileName;
    private String Path;
    private FileType type;

    public FileModel(String fileName, String Path, FileType type) {
        this.fileName = fileName;
        this.Path = Path;
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return Path;
    }

    public void setPath(String Path) {
        this.Path = Path;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public String getDirectoryPath() {
        if (type == FileType.DIRECTORY) {
            return Path;
        }
        return Path.replace(fileName, "");
    }

    public String getExtension() {
        if (type == FileType.DIRECTORY) {
            return "";
        }
        return fileName.split("\\.")[1];
    }
}
