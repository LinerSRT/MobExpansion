package net.mcskill.mobexpansion.drops;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Надёжная запись JSON-конфигов: пишем во временный файл, сбрасываем на диск и атомарно подменяем.
 * Предыдущая версия остаётся в {@code <имя>.bak} — если основной файл битый, читаем её.
 */
public final class SafeJsonFile {
    private SafeJsonFile() {
    }

    public static Path backupPath(Path path) {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    public static void write(Path path, Gson gson, JsonObject root) throws IOException {
        Files.createDirectories(path.getParent());
        final Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        final byte[] bytes = gson.toJson(root).getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            final ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining())
                channel.write(buffer);
            channel.force(true);
        }
        if (Files.isRegularFile(path) && isReadable(path, gson))
            Files.copy(path, backupPath(path), StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Читает JSON-объект. {@code null} — файла нет. Исключение — файл есть, но прочитать его не удалось.
     */
    @Nullable
    public static JsonObject read(Path path, Gson gson) throws IOException {
        if (!Files.isRegularFile(path))
            return null;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            final JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null)
                throw new IOException("empty file");
            return root;
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static boolean isReadable(Path path, Gson gson) {
        try {
            return read(path, gson) != null;
        } catch (IOException e) {
            return false;
        }
    }
}
