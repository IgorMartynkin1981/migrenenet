package ru.migrenenet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import ru.migrenenet.model.DiaryEntry;
import ru.migrenenet.model.DiaryEntry.Relief;
import ru.migrenenet.service.AnalyticsService;

class AnalyticsServiceTest {
    @Test
    void computesCriteria() {
        var list = new ArrayList<DiaryEntry>();
        for (int i = 0; i < 4; i++) {
            var e = new DiaryEntry();
            e.setPain(i < 3); e.setVas(i < 3 ? 6 : 0);
            e.setMedTaken(i < 2); e.setRelief(i == 0 ? Relief.FULL : i == 1 ? Relief.NONE : null);
            e.setFunctionLevel(i == 0 ? 3 : 0);
            list.add(e);
        }
        var s = AnalyticsService.compute(list);
        assertEquals(3, s.headacheDays());
        assertEquals(6.0, s.avgVas());
        assertEquals(2, s.medDays());
        assertEquals(1, s.limitedDays());
        assertEquals(50, s.reliefPercent());
    }
}
