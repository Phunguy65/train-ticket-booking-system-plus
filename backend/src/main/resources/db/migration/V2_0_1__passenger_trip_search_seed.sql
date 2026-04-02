CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_stations_search_trgm ON stations USING gin (
    (
        COALESCE(code, '') || ' ' || COALESCE(name, '') || ' ' || COALESCE(city, '')
    ) gin_trgm_ops
)
WHERE
    deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_scheduled_trips_departure_cursor ON scheduled_trips (departure_time, id)
WHERE
    deleted_at IS NULL;

INSERT INTO
    stations (id, code, name, city, created_at)
VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'HNO',
        'Ha Noi',
        'Ha Noi',
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'VIN',
        'Vinh',
        'Nghe An',
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'DAD',
        'Da Nang',
        'Da Nang',
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        'SGN',
        'Sai Gon',
        'Ho Chi Minh City',
        CURRENT_TIMESTAMP
    );

INSERT INTO
    trains (id, train_number, name, total_seats, created_at)
VALUES
    (
        '20000000-0000-0000-0000-000000000001',
        'SE1',
        'North South Express 1',
        8,
        CURRENT_TIMESTAMP
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        'SE2',
        'North South Express 2',
        8,
        CURRENT_TIMESTAMP
    );

INSERT INTO
    coaches (id, train_id, car_number, total_seats, created_at)
VALUES
    (
        '21000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        1,
        4,
        CURRENT_TIMESTAMP
    ),
    (
        '21000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000001',
        2,
        4,
        CURRENT_TIMESTAMP
    ),
    (
        '21000000-0000-0000-0000-000000000003',
        '20000000-0000-0000-0000-000000000002',
        1,
        4,
        CURRENT_TIMESTAMP
    ),
    (
        '21000000-0000-0000-0000-000000000004',
        '20000000-0000-0000-0000-000000000002',
        2,
        4,
        CURRENT_TIMESTAMP
    );

INSERT INTO
    seats (id, coach_id, seat_number, created_at)
VALUES
    (
        '22000000-0000-0000-0000-000000000001',
        '21000000-0000-0000-0000-000000000001',
        'A1',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000002',
        '21000000-0000-0000-0000-000000000001',
        'A2',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000003',
        '21000000-0000-0000-0000-000000000001',
        'A3',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000004',
        '21000000-0000-0000-0000-000000000001',
        'A4',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000005',
        '21000000-0000-0000-0000-000000000002',
        'B1',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000006',
        '21000000-0000-0000-0000-000000000002',
        'B2',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000007',
        '21000000-0000-0000-0000-000000000002',
        'B3',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000008',
        '21000000-0000-0000-0000-000000000002',
        'B4',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000009',
        '21000000-0000-0000-0000-000000000003',
        'A1',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000010',
        '21000000-0000-0000-0000-000000000003',
        'A2',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000011',
        '21000000-0000-0000-0000-000000000003',
        'A3',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000012',
        '21000000-0000-0000-0000-000000000003',
        'A4',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000013',
        '21000000-0000-0000-0000-000000000004',
        'B1',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000014',
        '21000000-0000-0000-0000-000000000004',
        'B2',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000015',
        '21000000-0000-0000-0000-000000000004',
        'B3',
        CURRENT_TIMESTAMP
    ),
    (
        '22000000-0000-0000-0000-000000000016',
        '21000000-0000-0000-0000-000000000004',
        'B4',
        CURRENT_TIMESTAMP
    );

INSERT INTO
    route_templates (
        id,
        origin_station_id,
        destination_station_id,
        base_price,
        created_at
    )
VALUES
    (
        '30000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000003',
        650000,
        CURRENT_TIMESTAMP
    ),
    (
        '30000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000004',
        950000,
        CURRENT_TIMESTAMP
    ),
    (
        '30000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000004',
        720000,
        CURRENT_TIMESTAMP
    );

INSERT INTO
    scheduled_trips (
        id,
        route_template_id,
        train_id,
        departure_time,
        arrival_time,
        status,
        created_at
    )
VALUES
    (
        '40000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        '2026-05-01T01:00:00Z',
        '2026-05-01T11:00:00Z',
        'SCHEDULED',
        CURRENT_TIMESTAMP
    ),
    (
        '40000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000002',
        '2026-05-02T02:00:00Z',
        '2026-05-02T18:00:00Z',
        'BOARDING',
        CURRENT_TIMESTAMP
    ),
    (
        '40000000-0000-0000-0000-000000000003',
        '30000000-0000-0000-0000-000000000003',
        '20000000-0000-0000-0000-000000000001',
        '2026-05-03T03:00:00Z',
        '2026-05-03T13:30:00Z',
        'SCHEDULED',
        CURRENT_TIMESTAMP
    );

INSERT INTO
    trip_seat_availability (scheduled_trip_id, seat_id, status, version)
SELECT
    '40000000-0000-0000-0000-000000000001',
    s.id,
    'AVAILABLE',
    1
FROM
    seats s
WHERE
    s.coach_id IN (
        '21000000-0000-0000-0000-000000000001',
        '21000000-0000-0000-0000-000000000002'
    );

INSERT INTO
    trip_seat_availability (scheduled_trip_id, seat_id, status, version)
SELECT
    '40000000-0000-0000-0000-000000000002',
    s.id,
    CASE
        WHEN s.seat_number IN ('A1', 'B1') THEN 'BOOKED'
        ELSE 'AVAILABLE'
    END,
    1
FROM
    seats s
WHERE
    s.coach_id IN (
        '21000000-0000-0000-0000-000000000003',
        '21000000-0000-0000-0000-000000000004'
    );

INSERT INTO
    trip_seat_availability (scheduled_trip_id, seat_id, status, version)
SELECT
    '40000000-0000-0000-0000-000000000003',
    s.id,
    CASE
        WHEN s.seat_number IN ('A1', 'A2', 'B1') THEN 'BOOKED'
        ELSE 'AVAILABLE'
    END,
    1
FROM
    seats s
WHERE
    s.coach_id IN (
        '21000000-0000-0000-0000-000000000001',
        '21000000-0000-0000-0000-000000000002'
    );
