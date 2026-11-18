package edu.campus;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

final class Photo {
    static final int MAX_BYTES = 4 * 1024 * 1024;
    private Photo() {}

    static byte[] load(Path path) throws IOException {
        byte[] data;
        try (InputStream input = Files.newInputStream(path)) {
            data = input.readNBytes(MAX_BYTES + 1);
        }
        if (data.length > MAX_BYTES) throw new IOException("Choose a photo no larger than 4 MB.");
        decode(data);
        return data;
    }

    static BufferedImage decode(byte[] data) throws IOException {
        try (var image = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (image == null) throw new IOException("The image could not be opened.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(image);
            if (!readers.hasNext()) throw new IOException("Choose a readable JPEG or PNG.");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!(format.equalsIgnoreCase("JPEG") || format.equalsIgnoreCase("PNG")))
                    throw new IOException("Choose a JPEG or PNG.");
                reader.setInput(image, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width > 6000 || height > 6000 || (long) width * height > 20_000_000)
                    throw new IOException("Photo dimensions are too large.");
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }
}
