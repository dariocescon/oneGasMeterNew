package com.aton.proj.oneGasMeter.repository;

import com.aton.proj.oneGasMeter.entity.DeviceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test integrazione per DeviceRegistryRepository con H2.
 */
@SpringBootTest
class DeviceRegistryRepositoryTest {

    @Autowired
    private DeviceRegistryRepository repository;

    @Test
    void saveAndFindBySerialNumber() {
        DeviceRegistry device = createTestDevice("TEST001");
        repository.save(device);

        Optional<DeviceRegistry> found = repository.findById("TEST001");
        assertTrue(found.isPresent());
        assertEquals("TEST001", found.get().getSerialNumber());
        assertEquals("RSE", found.get().getDeviceType());
    }

    @Test
    void updateFrameCounter() {
        DeviceRegistry device = createTestDevice("TEST002");
        repository.save(device);

        DeviceRegistry toUpdate = repository.findById("TEST002").orElseThrow();
        toUpdate.setFrameCounterTx(100);
        toUpdate.setFrameCounterRx(50);
        repository.save(toUpdate);

        DeviceRegistry updated = repository.findById("TEST002").orElseThrow();
        assertEquals(100, updated.getFrameCounterTx());
        assertEquals(50, updated.getFrameCounterRx());
    }

    @Test
    void findByIdReturnsEmptyForUnknown() {
        Optional<DeviceRegistry> found = repository.findById("UNKNOWN");
        assertTrue(found.isEmpty());
    }

    @Test
    void deleteDevice() {
        DeviceRegistry device = createTestDevice("TEST003");
        repository.save(device);
        repository.deleteById("TEST003");

        assertTrue(repository.findById("TEST003").isEmpty());
    }

    private DeviceRegistry createTestDevice(String serial) {
        DeviceRegistry device = new DeviceRegistry();
        device.setSerialNumber(serial);
        device.setDeviceType("RSE");
        device.setLogicalDeviceName("ATON" + serial);
        device.setEncryptionKeyEnc(new byte[44]);
        device.setAuthenticationKeyEnc(new byte[44]);
        device.setSystemTitle(new byte[]{0x53, 0x41, 0x43, 0x53, 0x41, 0x43, 0x53, 0x41});
        return device;
    }
}
