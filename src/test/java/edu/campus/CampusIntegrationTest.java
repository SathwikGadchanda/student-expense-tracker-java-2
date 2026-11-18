package edu.campus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.*;
import static edu.campus.Item.*;
import static org.junit.jupiter.api.Assertions.*;

class CampusIntegrationTest {
    @TempDir Path temp;
    private Path file;
    private CampusService service;

    @BeforeEach void setup() throws Exception {
        file = temp.resolve("db/campus.db");
        service = reopen();
    }

    private CampusService reopen() throws Exception {
        SqliteRepository repository = new SqliteRepository(file);
        repository.initialize();
        return new CampusService(repository);
    }

    private Draft draft(Kind kind, String name, byte[] photo) {
        return new Draft(kind, name, Category.ELECTRONICS, "Library", LocalDate.now(),
                "Grey case with one identifying mark.", photo);
    }

    @Test void initializesEmptyDatabase() throws Exception { assertTrue(service.all().isEmpty()); }

    @Test void recordsBothTypesAndReturnsGeneratedIds() throws Exception {
        long lost = service.add(draft(Kind.LOST, "Phone", null));
        long found = service.add(draft(Kind.FOUND, "Charger", null));
        assertTrue(lost > 0 && found > lost);
        assertEquals(Kind.LOST, service.details(lost).item().kind());
        assertEquals(Status.OPEN, service.details(found).item().status());
    }

    @Test void editingOpenItemPreservesItsId() throws Exception {
        long id = service.add(draft(Kind.LOST, "Phone", null));
        service.edit(id, draft(Kind.LOST, "Black phone", null));
        assertEquals(id, service.all().get(0).id());
        assertEquals("Black phone", service.all().get(0).title());
    }

    @Test void searchCombinesTypeCategoryStatusAndKeywords() throws Exception {
        service.add(draft(Kind.LOST, "USB cable", null));
        long id = service.add(draft(Kind.FOUND, "Charger", null));
        service.recordClaim(id, "Demo Student", "Described the adapter label.");
        var items = service.all();
        assertEquals(1, service.matching(items, " LIBRARY ", Kind.FOUND, Category.ELECTRONICS, Status.CLAIMED).size());
        assertTrue(service.matching(items, "library", Kind.FOUND, Category.BOOKS, null).isEmpty());
        assertTrue(service.matching(items, "cable", Kind.LOST, null, Status.CLAIMED).isEmpty());
    }

    @Test void validPngSurvivesReopen() throws Exception {
        var buffer = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 6, BufferedImage.TYPE_INT_RGB), "png", buffer);
        long id = service.add(draft(Kind.FOUND, "USB drive", buffer.toByteArray()));
        assertArrayEquals(buffer.toByteArray(), reopen().details(id).photo());
    }

    @Test void rejectsInvalidPhotosBeforeWriting() throws Exception {
        assertThrows(IOException.class, () -> service.add(draft(Kind.FOUND, "USB drive", new byte[]{1, 2, 3})));
        assertTrue(service.all().isEmpty());
    }

    @Test void rejectsBlankFieldsFutureDatesAndOversizedPhotos() {
        assertThrows(IllegalArgumentException.class, () -> draft(Kind.FOUND, " ", null));
        assertThrows(IllegalArgumentException.class, () -> draft(Kind.FOUND, "Phone", new byte[Photo.MAX_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> new Draft(Kind.LOST, "Phone", Category.OTHER,
                "Library", LocalDate.now().plusDays(1), "Black cover", null));
    }

    @Test void claimAndReturnArePersistedWithTimestamps() throws Exception {
        long id = service.add(draft(Kind.FOUND, "Phone", null));
        service.recordClaim(id, "Demo Student", "Identified the case.");
        assertEquals(Status.CLAIMED, service.details(id).item().status());
        service.markReturned(id);
        Details reopened = reopen().details(id);
        assertEquals(Status.RETURNED, reopened.item().status());
        assertEquals("Demo Student", reopened.claim().claimant());
        assertNotNull(reopened.claim().recordedAt());
        assertNotNull(reopened.claim().returnedAt());
    }

    @Test void preventsLostClaimsAndUnconfirmedReturns() throws Exception {
        long lost = service.add(draft(Kind.LOST, "Phone", null));
        long found = service.add(draft(Kind.FOUND, "Cable", null));
        assertThrows(IllegalArgumentException.class, () -> service.recordClaim(lost, "Demo", "Verified"));
        assertThrows(IllegalArgumentException.class, () -> service.markReturned(found));
        assertEquals(Status.OPEN, service.details(found).item().status());
    }

    @Test void duplicateClaimsAndEditsAfterClaimAreRejected() throws Exception {
        long id = service.add(draft(Kind.FOUND, "Phone", null));
        service.recordClaim(id, "Demo Student", "Identified the case.");
        assertThrows(IllegalArgumentException.class, () -> service.recordClaim(id, "Second", "Another claim"));
        assertThrows(IllegalArgumentException.class, () -> service.edit(id, draft(Kind.FOUND, "Changed phone", null)));
        assertEquals("Demo Student", service.details(id).claim().claimant());
    }

    @Test void rollsBackStatusIfClaimInsertFails() throws Exception {
        long id = service.add(draft(Kind.FOUND, "Phone", null));
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var s = c.createStatement()) {
            s.execute("CREATE TRIGGER reject_claim BEFORE INSERT ON claims BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        }
        assertThrows(SQLException.class, () -> service.recordClaim(id, "Demo", "Verified"));
        assertEquals(Status.OPEN, service.details(id).item().status());
        assertNull(service.details(id).claim());
    }

    @Test void competingClaimsHaveExactlyOneWinner() throws Exception {
        long id = service.add(draft(Kind.FOUND, "Phone", null));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            gate.await();
            try {
                service.recordClaim(id, Thread.currentThread().getName(), "Ownership details");
                return true;
            } catch (IllegalArgumentException expected) {
                return false;
            }
        };
        try {
            Future<Boolean> first = pool.submit(attempt), second = pool.submit(attempt);
            gate.countDown();
            assertNotEquals(first.get(8, TimeUnit.SECONDS), second.get(8, TimeUnit.SECONDS));
            assertEquals(Status.CLAIMED, service.details(id).item().status());
        } finally {
            pool.shutdownNow();
        }
    }
}
