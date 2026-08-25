package com.loanpro.infrastructure.numbering;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class ApplicationNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final JdbcTemplate jdbcTemplate;

    public ApplicationNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String next() {
        Long seq = jdbcTemplate.queryForObject("SELECT nextval('application_number_seq')", Long.class);
        long value = seq == null ? 1L : seq;
        return "LN-%s-%06d".formatted(LocalDate.now().format(DATE), value);
    }
}
