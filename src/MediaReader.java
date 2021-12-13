import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class MediaReader {

    private static MediaReader singleton;

    public static MediaReader getInstance() {
        if (singleton == null) {
            singleton = new MediaReader();
        }
        return singleton;
    }

    private final int imageWidth = 352;
    private final int imageHeight = 288;
    private final int pixel_num;

    private MediaReader() {
        pixel_num = imageWidth * imageHeight;
    }

    public ArrayList<File> readVideoDir(String directory) {
        ArrayList<File> list = new ArrayList<>();

        File selectedFolder = new File(directory);
        File[] files = selectedFolder.listFiles();
        if (files != null) {
            Arrays.sort(files, (o1, o2) -> {
                if (o1.getName().length() == o2.getName().length()) {
                    return o1.getName().compareTo(o2.getName());
                }
                return o1.getName().length() - o2.getName().length();
            });

            for (File file : files) {
                if (file.getAbsolutePath().endsWith(".rgb")) {
                    list.add(file);
                }
            }
        }

        System.out.println("Total Images Added: " + list.size());
        return list;
    }

    public BufferedImage readRgbFile(File imageFile) {
        if (!imageFile.getName().endsWith(".rgb")) {
            return null;
        }
        try {
            InputStream fileStream = new FileInputStream(imageFile);
            int len = (int) imageFile.length();
            byte[] bytes = new byte[len];

            int offset = 0;
            int numberRead;
            while (offset < bytes.length && (numberRead = fileStream.read(bytes, offset,len-offset)) >= 0) {
                offset = offset + numberRead;
            }
            fileStream.close();

            BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
            int index = 0;
            for (int y = 0; y < imageHeight; y++) {
                for (int x = 0; x < imageWidth; x++) {
                    byte r = bytes[index];
                    byte g = bytes[index + pixel_num];
                    byte b = bytes[index + pixel_num + pixel_num];
                    int pixel = 0XFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                    image.setRGB(x, y, pixel);
                    index = index + 1;
                }
            }
            return image;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public BufferedInputStream readWavFile(String waveFile) {
        File file = new File(waveFile);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                return null;
            }
            for (File f : files) {
                if (f.getName().endsWith(".wav")) {
                    waveFile = f.getAbsolutePath();
                    file = new File(waveFile);
                    break;
                }
            }
        }
        if (file.isFile()) {
            try {
                InputStream inputStream = new FileInputStream(waveFile);
                return new BufferedInputStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
