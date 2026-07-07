package com.english.service;

import com.english.dto.ExamCountdown;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * 根据各考试的固定规律自动计算下一场考试日期与倒计时，无需依赖外部网站，永不失效。
 */
@Service
public class CountdownService {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public ExamCountdown getCountdown(String code) {
        LocalDate today = LocalDate.now();
        LocalDate examDate;
        String label;

        switch (code == null ? "" : code) {
            case "cet4":
            case "cet6":
                examDate = nextCetDate(today);
                label = "下次考试";
                break;
            case "kaoyan":
                examDate = nextKaoyanDate(today);
                label = "初试时间";
                break;
            case "toefl":
            case "ielts":
            case "gre":
                examDate = nextFrequentSession(today);
                label = "最近场次";
                break;
            default:
                return new ExamCountdown("考试安排", "待更新", "敬请期待", false);
        }

        long diffDays = ChronoUnit.DAYS.between(today, examDate);
        String countdownText;
        if (diffDays > 0) {
            countdownText = "倒计时 " + diffDays + " 天";
        } else if (diffDays == 0) {
            countdownText = "今天考试";
        } else {
            countdownText = "已开考";
        }

        boolean urgent = diffDays >= 0 && diffDays <= 30;
        return new ExamCountdown(label, examDate.format(DISPLAY), countdownText, urgent);
    }

    /** CET 笔试：每年 6 月和 12 月的第三个星期六。 */
    private LocalDate nextCetDate(LocalDate today) {
        int year = today.getYear();
        LocalDate june = thirdSaturday(year, 6);
        LocalDate december = thirdSaturday(year, 12);
        if (!today.isAfter(june)) return june;
        if (!today.isAfter(december)) return december;
        return thirdSaturday(year + 1, 6);
    }

    /** 考研初试：每年 12 月的最后一个星期六。 */
    private LocalDate nextKaoyanDate(LocalDate today) {
        int year = today.getYear();
        LocalDate exam = lastSaturday(year, 12);
        if (today.isAfter(exam)) {
            exam = lastSaturday(year + 1, 12);
        }
        return exam;
    }

    /** 托福/雅思/GRE 场次频繁，取距今至少 14 天的下一个星期六作为代表场次。 */
    private LocalDate nextFrequentSession(LocalDate today) {
        LocalDate base = today.plusDays(14);
        return base.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
    }

    private LocalDate thirdSaturday(int year, int month) {
        return LocalDate.of(year, month, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.SATURDAY))
                .plusWeeks(2);
    }

    private LocalDate lastSaturday(int year, int month) {
        return LocalDate.of(year, month, 1)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.SATURDAY));
    }
}
