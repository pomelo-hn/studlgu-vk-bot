package com.studlgu.vkbot.service.handler.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MotivationRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void addStoresTrimmedMotivationText() throws Exception {
        MotivationRepository repository = new MotivationRepository(tempDir.resolve("motivations.json").toString());
        repository.init();

        repository.add("  Ты справишься!  ");

        assertThat(repository.findAll()).containsExactly("Ты справишься!");
    }

    @Test
    void addIgnoresBlankMotivationText() throws Exception {
        MotivationRepository repository = new MotivationRepository(tempDir.resolve("motivations.json").toString());
        repository.init();

        repository.add("   ");

        assertThat(repository.findAll()).isEmpty();
    }
}
