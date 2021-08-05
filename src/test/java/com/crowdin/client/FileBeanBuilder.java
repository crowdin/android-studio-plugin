package com.crowdin.client;

public class FileBeanBuilder {

    private FileBean fb;

    private FileBeanBuilder(FileBean fb) {
        this.fb = fb;
    }

    public static FileBeanBuilder fileBean(String source, String translation) {
        FileBean fb = new FileBean();
        fb.setSource(source);
        fb.setTranslation(translation);
        return new FileBeanBuilder(fb);
    }

    public FileBean build() {
        return fb;
    }
}
