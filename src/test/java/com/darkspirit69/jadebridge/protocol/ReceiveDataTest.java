package com.darkspirit69.jadebridge.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkspirit69.jadebridge.JadeProtocol;
import com.darkspirit69.jadebridge.TestEnv;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ReceiveDataTest {

    @BeforeAll
    static void boot() {
        TestEnv.boot();
    }

    private static CompoundTag tagWithChildren(int children, int valueSize) {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < children; i++) {
            tag.put("key" + i, StringTag.valueOf("x".repeat(valueSize)));
        }
        return tag;
    }

    @Test
    void smallTagsAreUntouched() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", 1);
        ReceiveData.trim(tag);
        assertEquals(1, tag.getIntOr("x", -1));
    }

    @Test
    void oversizedTagsAreTrimmedBelowLimit() {
        // 12 children x ~2 KiB: ~24 KiB total, each round removes one ~2 KiB child
        CompoundTag tag = tagWithChildren(12, 2048);
        assertTrue(tag.sizeInBytes() > JadeProtocol.MAX_PAYLOAD_SIZE);
        ReceiveData.trim(tag);
        assertTrue(tag.sizeInBytes() <= JadeProtocol.MAX_PAYLOAD_SIZE);
        assertTrue(tag.size() >= 1, "trimming removes whole children, not everything");
    }

    @Test
    void trimIsCappedAtTenRounds() {
        // one removal per round, hard cap of 10 rounds even if still oversized
        CompoundTag tag = tagWithChildren(200, 2048);
        ReceiveData.trim(tag);
        assertEquals(190, tag.size());
        assertTrue(tag.sizeInBytes() > JadeProtocol.MAX_PAYLOAD_SIZE);
    }

    @Test
    void trimDescendsIntoLargestCompoundChild() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("small", 1);
        CompoundTag child = new CompoundTag();
        for (int i = 0; i < 40; i++) {
            child.put("inner" + i, StringTag.valueOf("y".repeat(1024)));
        }
        tag.put("bigChild", child);

        ReceiveData.trim(tag);

        // the compound child is the largest entry, so each of the 10 rounds descends
        // into it and removes one inner string instead of the compound itself
        assertTrue(tag.contains("bigChild"), "one level of descent keeps the compound parent");
        assertEquals(30, child.size());
        assertTrue(tag.sizeInBytes() > JadeProtocol.MAX_PAYLOAD_SIZE, "cap reached while still oversized");
        assertEquals(1, tag.getIntOr("small", -1));
    }

    @Test
    void emptyTagSurvives() {
        CompoundTag tag = new CompoundTag();
        ReceiveData.trim(tag);
        assertTrue(tag.isEmpty());
    }
}
