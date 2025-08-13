package com.example.store.util;

import com.example.store.dto.SortEnumDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("unit")
@DisplayName("PageableBuilder - {Unit}")
class PageableBuilderTest {

    private final PageableBuilder pageableBuilder = new PageableBuilder();
    private final int defaultSize = 10;
    private final String defaultSortBy = "id";
    private final String defaultSortDir = "desc";

    @Test
    @DisplayName("Should build pageable with all non-null parameters")
    void shouldBuildPageableWithAllNonNullParameters() {
        // Given
        Integer page = 2;
        Integer size = 20;
        String sortBy = "name";
        SortEnumDTO sortDir = SortEnumDTO.asc;

        // When
        Pageable pageable = pageableBuilder.buildPageable(
                page, size, sortBy, sortDir,
                defaultSize, defaultSortBy, defaultSortDir
        );

        // Then
        assertNotNull(pageable);
        assertEquals(page, pageable.getPageNumber());
        assertEquals(size, pageable.getPageSize());
        assertEquals(sortBy, pageable.getSort().getOrderFor(sortBy).getProperty());
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor(sortBy).getDirection());
    }

    @Test
    @DisplayName("Should build pageable with all null parameters using defaults")
    void shouldBuildPageableWithAllNullParametersUsingDefaults() {
        // When
        Pageable pageable = pageableBuilder.buildPageable(
                null, null, null, null,
                defaultSize, defaultSortBy, defaultSortDir
        );

        // Then
        assertNotNull(pageable);
        assertEquals(0, pageable.getPageNumber());
        assertEquals(defaultSize, pageable.getPageSize());
        assertEquals(defaultSortBy, pageable.getSort().getOrderFor(defaultSortBy).getProperty());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor(defaultSortBy).getDirection());
    }

    @Test
    @DisplayName("Should build pageable with ASC sort direction")
    void shouldBuildPageableWithAscSortDirection() {
        // Given
        String sortDirValue = "asc";

        // When
        Pageable pageable = pageableBuilder.buildPageable(
                0, 10, "id", null,
                defaultSize, defaultSortBy, sortDirValue
        );

        // Then
        assertNotNull(pageable);
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("id").getDirection());
    }

    @Test
    @DisplayName("Should build pageable with DESC sort direction")
    void shouldBuildPageableWithDescSortDirection() {
        // Given
        String sortDirValue = "desc";

        // When
        Pageable pageable = pageableBuilder.buildPageable(
                0, 10, "id", null,
                defaultSize, defaultSortBy, sortDirValue
        );

        // Then
        assertNotNull(pageable);
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("id").getDirection());
    }

    @ParameterizedTest
    @MethodSource("provideNullAndNonNullParameters")
    @DisplayName("Should handle various combinations of null and non-null parameters")
    void shouldHandleVariousCombinationsOfNullAndNonNullParameters(
            Integer page, Integer size, String sortBy, SortEnumDTO sortDir,
            int expectedPage, int expectedSize, String expectedSortBy, Sort.Direction expectedDirection) {
        
        // When
        Pageable pageable = pageableBuilder.buildPageable(
                page, size, sortBy, sortDir,
                defaultSize, defaultSortBy, defaultSortDir
        );

        // Then
        assertNotNull(pageable);
        assertEquals(expectedPage, pageable.getPageNumber());
        assertEquals(expectedSize, pageable.getPageSize());
        assertEquals(expectedSortBy, pageable.getSort().getOrderFor(expectedSortBy).getProperty());
        assertEquals(expectedDirection, pageable.getSort().getOrderFor(expectedSortBy).getDirection());
    }

    private static Stream<Arguments> provideNullAndNonNullParameters() {
        return Stream.of(
                // page, size, sortBy, sortDir, expectedPage, expectedSize, expectedSortBy, expectedDirection
                Arguments.of(null, 20, "name", SortEnumDTO.asc, 0, 20, "name", Sort.Direction.ASC),
                Arguments.of(2, null, "name", SortEnumDTO.asc, 2, 10, "name", Sort.Direction.ASC),
                Arguments.of(2, 20, null, SortEnumDTO.asc, 2, 20, "id", Sort.Direction.ASC),
                Arguments.of(2, 20, "name", null, 2, 20, "name", Sort.Direction.DESC),
                Arguments.of(null, null, "name", SortEnumDTO.asc, 0, 10, "name", Sort.Direction.ASC),
                Arguments.of(null, null, null, SortEnumDTO.asc, 0, 10, "id", Sort.Direction.ASC),
                Arguments.of(null, null, null, null, 0, 10, "id", Sort.Direction.DESC)
        );
    }
}