/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.storage.migration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImgurToMinioRehostCliTest {

    @Mock
    private ImgurToMinioRehostService rehostService;

    private ImgurToMinioRehostCli cli;

    @BeforeEach
    void setUp() {
        cli = new ImgurToMinioRehostCli(rehostService);
    }

    @Test
    void runIgnoresWhenNoFlagsProvided() throws Exception {
        cli.run("unrelated", "args");
        verifyNoInteractions(rehostService);
    }

    @Test
    void runExecutesWhenRehostFlagProvided() throws Exception {
        when(rehostService.rehostAll(anyBoolean(), anyInt(), anyLong()))
                .thenReturn(new ImgurToMinioRehostService.RehostReport(0, 0, 0, 0, 0, false, List.of()));

        cli.run("--rehost-imgur");
        verify(rehostService).rehostAll(eq(false), eq(0), eq(100L));
    }

    @Test
    void runExecutesDryRunWhenFlagSpecified() throws Exception {
        when(rehostService.rehostAll(anyBoolean(), anyInt(), anyLong()))
                .thenReturn(new ImgurToMinioRehostService.RehostReport(0, 0, 0, 0, 0, false, List.of()));

        cli.run("--rehost-imgur=dry-run");
        verify(rehostService).rehostAll(eq(true), eq(0), eq(100L));
    }

    @Test
    void runParsesLimitAndDelayArguments() throws Exception {
        when(rehostService.rehostAll(anyBoolean(), anyInt(), anyLong()))
                .thenReturn(new ImgurToMinioRehostService.RehostReport(0, 0, 0, 0, 0, false, List.of()));

        cli.run("--rehost-imgur", "--dry-run", "--rehost-limit=25", "--rehost-delay=50");
        verify(rehostService).rehostAll(eq(true), eq(25), eq(50L));
    }
}
