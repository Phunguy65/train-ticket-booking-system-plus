#!/usr/bin/env python3
"""
Generate seed data for train ticket booking system.

This script generates realistic Vietnamese train station data and outputs
a SQL file that can be used as a Flyway repeatable migration.

Usage:
    python generate.py [--output OUTPUT_PATH] [--days DAYS] [--seed SEED]

Example:
    python generate.py --output ../../backend/src/main/resources/db/migration/R__seed_dev_data.sql
"""

import argparse
import random
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import NamedTuple

from uuid6 import uuid7

# =============================================================================
# Vietnamese Train Station Data (North-South Railway)
# =============================================================================

class Station(NamedTuple):
    code: str
    name: str
    city: str
    km_from_hanoi: int  # Distance from Hanoi for price calculation


# Real Vietnamese train stations on the North-South railway
VIETNAMESE_STATIONS = [
    Station("HAN", "Ga Hà Nội", "Hà Nội", 0),
    Station("PYE", "Ga Phủ Lý", "Hà Nam", 58),
    Station("NDI", "Ga Nam Định", "Nam Định", 87),
    Station("NBH", "Ga Ninh Bình", "Ninh Bình", 116),
    Station("THA", "Ga Thanh Hóa", "Thanh Hóa", 175),
    Station("VIN", "Ga Vinh", "Nghệ An", 319),
    Station("DHA", "Ga Đồng Hới", "Quảng Bình", 522),
    Station("DNG", "Ga Đông Hà", "Quảng Trị", 601),
    Station("HUE", "Ga Huế", "Thừa Thiên Huế", 688),
    Station("DAN", "Ga Đà Nẵng", "Đà Nẵng", 791),
    Station("TKY", "Ga Tam Kỳ", "Quảng Nam", 865),
    Station("QNG", "Ga Quảng Ngãi", "Quảng Ngãi", 928),
    Station("DPH", "Ga Diêu Trì", "Bình Định", 1047),
    Station("TUH", "Ga Tuy Hòa", "Phú Yên", 1130),
    Station("NTG", "Ga Nha Trang", "Khánh Hòa", 1315),
    Station("THP", "Ga Tháp Chàm", "Ninh Thuận", 1408),
    Station("BTH", "Ga Bình Thuận", "Bình Thuận", 1518),
    Station("BHO", "Ga Biên Hòa", "Đồng Nai", 1698),
    Station("SGN", "Ga Sài Gòn", "Thành phố Hồ Chí Minh", 1726),
]

# Train configurations
TRAIN_CONFIGS = [
    {"number": "SE1", "name": "Thống Nhất 1", "coaches": 5, "seats_per_coach": 28},
    {"number": "SE2", "name": "Thống Nhất 2", "coaches": 5, "seats_per_coach": 28},
    {"number": "SE3", "name": "Thống Nhất 3", "coaches": 5, "seats_per_coach": 28},
    {"number": "SE4", "name": "Thống Nhất 4", "coaches": 5, "seats_per_coach": 28},
    {"number": "SE5", "name": "Thống Nhất 5", "coaches": 4, "seats_per_coach": 24},
    {"number": "SE6", "name": "Thống Nhất 6", "coaches": 4, "seats_per_coach": 24},
    {"number": "SE7", "name": "Thống Nhất 7", "coaches": 4, "seats_per_coach": 24},
    {"number": "SE8", "name": "Thống Nhất 8", "coaches": 4, "seats_per_coach": 24},
    {"number": "TN1", "name": "Tàu Nhanh 1", "coaches": 3, "seats_per_coach": 20},
    {"number": "TN2", "name": "Tàu Nhanh 2", "coaches": 3, "seats_per_coach": 20},
]

# Generate ALL station pairs as routes
def _build_all_routes():
    """Build routes for every (origin, destination) pair with trips_per_day based on distance."""
    routes = []
    num_stations = len(VIETNAMESE_STATIONS)
    for i in range(num_stations):
        for j in range(num_stations):
            if i == j:
                continue
            distance = abs(VIETNAMESE_STATIONS[i].km_from_hanoi - VIETNAMESE_STATIONS[j].km_from_hanoi)
            if distance >= 1000:
                trips_per_day = 3
            elif distance >= 500:
                trips_per_day = 2
            else:
                trips_per_day = 1
            routes.append((i, j, trips_per_day))
    return routes


MAJOR_ROUTES = _build_all_routes()

# Departure times (hour, minute) for different trip slots
DEPARTURE_TIMES = [
    (6, 0),    # Morning
    (13, 30),  # Afternoon
    (19, 0),   # Evening
    (22, 30),  # Night
]

# Price per km (VND)
PRICE_PER_KM = 550


# =============================================================================
# Data Generation
# =============================================================================

class DataGenerator:
    def __init__(self, seed: int = 12345):
        random.seed(seed)
        self.stations: list[tuple[str, Station]] = []  # (uuid, station)
        self.trains: list[tuple[str, dict]] = []  # (uuid, config)
        self.coaches: list[tuple[str, str, int, int]] = []  # (uuid, train_uuid, car_number, total_seats)
        self.seats: list[tuple[str, str, str]] = []  # (uuid, coach_uuid, seat_number)
        self.route_templates: list[tuple[str, str, str, int]] = []  # (uuid, origin_uuid, dest_uuid, price)
        self.scheduled_trips: list[tuple[str, str, str, datetime, datetime, str]] = []
        self.trip_seat_availability: list[tuple[str, str]] = []  # (trip_uuid, seat_uuid)

    def generate_all(self, days: int = 30):
        """Generate all seed data."""
        print("Generating stations...")
        self._generate_stations()
        
        print("Generating trains, coaches, and seats...")
        self._generate_trains()
        
        print("Generating route templates...")
        self._generate_route_templates()
        
        print(f"Generating scheduled trips for {days} days...")
        self._generate_scheduled_trips(days)
        
        print("Generating trip seat availability...")
        self._generate_trip_seat_availability()
        
        print(f"\nSummary:")
        print(f"  Stations: {len(self.stations)}")
        print(f"  Trains: {len(self.trains)}")
        print(f"  Coaches: {len(self.coaches)}")
        print(f"  Seats: {len(self.seats)}")
        print(f"  Route templates: {len(self.route_templates)}")
        print(f"  Scheduled trips: {len(self.scheduled_trips)}")
        print(f"  Trip seat availability: {len(self.trip_seat_availability)}")

    def _generate_stations(self):
        for station in VIETNAMESE_STATIONS:
            self.stations.append((str(uuid7()), station))

    def _generate_trains(self):
        for config in TRAIN_CONFIGS:
            train_uuid = str(uuid7())
            total_seats = config["coaches"] * config["seats_per_coach"]
            self.trains.append((train_uuid, {**config, "total_seats": total_seats}))
            
            # Generate coaches for this train
            for car_num in range(1, config["coaches"] + 1):
                coach_uuid = str(uuid7())
                self.coaches.append((coach_uuid, train_uuid, car_num, config["seats_per_coach"]))
                
                # Generate seats for this coach
                self._generate_seats_for_coach(coach_uuid, config["seats_per_coach"])

    def _generate_seats_for_coach(self, coach_uuid: str, total_seats: int):
        """Generate seats with pattern: A1, A2, ..., B1, B2, ..., C1, C2, ..."""
        rows = ["A", "B", "C", "D"]
        seats_per_row = (total_seats + len(rows) - 1) // len(rows)
        
        seat_count = 0
        for row in rows:
            for num in range(1, seats_per_row + 1):
                if seat_count >= total_seats:
                    break
                seat_uuid = str(uuid7())
                seat_number = f"{row}{num}"
                self.seats.append((seat_uuid, coach_uuid, seat_number))
                seat_count += 1

    def _generate_route_templates(self):
        for origin_idx, dest_idx, _ in MAJOR_ROUTES:
            origin_station = self.stations[origin_idx]
            dest_station = self.stations[dest_idx]
            
            # Calculate price based on distance
            distance = abs(origin_station[1].km_from_hanoi - dest_station[1].km_from_hanoi)
            base_price = distance * PRICE_PER_KM
            # Round to nearest 10,000 VND
            base_price = round(base_price / 10000) * 10000
            
            route_uuid = str(uuid7())
            self.route_templates.append((
                route_uuid,
                origin_station[0],
                dest_station[0],
                base_price
            ))

    def _generate_scheduled_trips(self, days: int):
        start_date = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
        start_date += timedelta(days=1)  # Start from tomorrow
        
        # Track which train is assigned to which route
        train_assignment = {}
        train_index = 0
        
        for day_offset in range(days):
            current_date = start_date + timedelta(days=day_offset)
            
            for route_idx, (origin_idx, dest_idx, trips_per_day) in enumerate(MAJOR_ROUTES):
                route_template = self.route_templates[route_idx]
                origin_station = self.stations[origin_idx]
                dest_station = self.stations[dest_idx]
                
                # Calculate travel time based on distance (average 50 km/h)
                distance = abs(origin_station[1].km_from_hanoi - dest_station[1].km_from_hanoi)
                travel_hours = distance / 50
                
                for trip_num in range(trips_per_day):
                    # Assign train (round-robin)
                    if (route_idx, trip_num) not in train_assignment:
                        train_assignment[(route_idx, trip_num)] = train_index % len(self.trains)
                        train_index += 1
                    
                    assigned_train = self.trains[train_assignment[(route_idx, trip_num)]]
                    
                    # Get departure time
                    dep_hour, dep_minute = DEPARTURE_TIMES[trip_num % len(DEPARTURE_TIMES)]
                    departure_time = current_date.replace(hour=dep_hour, minute=dep_minute)
                    arrival_time = departure_time + timedelta(hours=travel_hours)
                    
                    # Determine status based on date
                    days_from_now = day_offset
                    if days_from_now < 0:
                        status = "DEPARTED"
                    elif days_from_now == 0:
                        status = random.choice(["SCHEDULED", "BOARDING"])
                    else:
                        status = "SCHEDULED"
                    
                    trip_uuid = str(uuid7())
                    self.scheduled_trips.append((
                        trip_uuid,
                        route_template[0],
                        assigned_train[0],
                        departure_time,
                        arrival_time,
                        status
                    ))

    def _generate_trip_seat_availability(self):
        # Map coach to train
        coach_to_train = {}
        for coach_uuid, train_uuid, _, _ in self.coaches:
            coach_to_train[coach_uuid] = train_uuid
        
        # Map seat to coach
        seat_to_coach = {}
        for seat_uuid, coach_uuid, _ in self.seats:
            seat_to_coach[seat_uuid] = coach_uuid
        
        # For each trip, add seat availability for all seats of the assigned train
        for trip in self.scheduled_trips:
            trip_uuid = trip[0]
            train_uuid = trip[2]
            
            # Find all seats for this train
            for seat_uuid, coach_uuid, _ in self.seats:
                if coach_to_train.get(coach_uuid) == train_uuid:
                    self.trip_seat_availability.append((trip_uuid, seat_uuid))

    def to_sql(self) -> str:
        """Generate SQL output."""
        lines = []
        lines.append("-- ============================================================")
        lines.append("-- REPEATABLE MIGRATION: Seed Development Data")
        lines.append(f"-- Generated by scripts/generate_seed_data/generate.py")
        lines.append(f"-- Generated at: {datetime.now(timezone.utc).isoformat()}")
        lines.append("-- ============================================================")
        lines.append("")
        lines.append("-- WARNING: This will delete all existing data in these tables!")
        lines.append("-- Only use in development environment.")
        lines.append("")
        lines.append("-- Clear existing data (respect FK order)")
        lines.append("DELETE FROM trip_seat_availability;")
        lines.append("DELETE FROM scheduled_trips;")
        lines.append("DELETE FROM route_templates;")
        lines.append("DELETE FROM seats;")
        lines.append("DELETE FROM coaches;")
        lines.append("DELETE FROM trains;")
        lines.append("DELETE FROM stations;")
        lines.append("")
        
        # Stations
        lines.append("-- ============================================================")
        lines.append("-- STATIONS")
        lines.append("-- ============================================================")
        lines.append("INSERT INTO stations (id, code, name, city, created_at) VALUES")
        station_values = []
        for uuid, station in self.stations:
            station_values.append(
                f"    ('{uuid}', '{station.code}', '{self._escape(station.name)}', "
                f"'{self._escape(station.city)}', CURRENT_TIMESTAMP)"
            )
        lines.append(",\n".join(station_values) + ";")
        lines.append("")
        
        # Trains
        lines.append("-- ============================================================")
        lines.append("-- TRAINS")
        lines.append("-- ============================================================")
        lines.append("INSERT INTO trains (id, train_number, name, total_seats, created_at) VALUES")
        train_values = []
        for uuid, config in self.trains:
            train_values.append(
                f"    ('{uuid}', '{config['number']}', '{self._escape(config['name'])}', "
                f"{config['total_seats']}, CURRENT_TIMESTAMP)"
            )
        lines.append(",\n".join(train_values) + ";")
        lines.append("")
        
        # Coaches
        lines.append("-- ============================================================")
        lines.append("-- COACHES")
        lines.append("-- ============================================================")
        lines.append("INSERT INTO coaches (id, train_id, car_number, total_seats, created_at) VALUES")
        coach_values = []
        for uuid, train_uuid, car_number, total_seats in self.coaches:
            coach_values.append(
                f"    ('{uuid}', '{train_uuid}', {car_number}, {total_seats}, CURRENT_TIMESTAMP)"
            )
        lines.append(",\n".join(coach_values) + ";")
        lines.append("")
        
        # Seats (batch insert for performance)
        lines.append("-- ============================================================")
        lines.append("-- SEATS")
        lines.append("-- ============================================================")
        # Split into batches of 500 for large inserts
        batch_size = 500
        for i in range(0, len(self.seats), batch_size):
            batch = self.seats[i:i + batch_size]
            lines.append("INSERT INTO seats (id, coach_id, seat_number, created_at) VALUES")
            seat_values = []
            for uuid, coach_uuid, seat_number in batch:
                seat_values.append(
                    f"    ('{uuid}', '{coach_uuid}', '{seat_number}', CURRENT_TIMESTAMP)"
                )
            lines.append(",\n".join(seat_values) + ";")
            lines.append("")
        
        # Route Templates
        lines.append("-- ============================================================")
        lines.append("-- ROUTE TEMPLATES")
        lines.append("-- ============================================================")
        lines.append("INSERT INTO route_templates (id, origin_station_id, destination_station_id, base_price, created_at) VALUES")
        route_values = []
        for uuid, origin_uuid, dest_uuid, price in self.route_templates:
            route_values.append(
                f"    ('{uuid}', '{origin_uuid}', '{dest_uuid}', {price}, CURRENT_TIMESTAMP)"
            )
        lines.append(",\n".join(route_values) + ";")
        lines.append("")
        
        # Scheduled Trips (batch insert)
        lines.append("-- ============================================================")
        lines.append("-- SCHEDULED TRIPS")
        lines.append("-- ============================================================")
        for i in range(0, len(self.scheduled_trips), batch_size):
            batch = self.scheduled_trips[i:i + batch_size]
            lines.append("INSERT INTO scheduled_trips (id, route_template_id, train_id, departure_time, arrival_time, status, created_at) VALUES")
            trip_values = []
            for uuid, route_uuid, train_uuid, dep_time, arr_time, status in batch:
                trip_values.append(
                    f"    ('{uuid}', '{route_uuid}', '{train_uuid}', "
                    f"'{dep_time.strftime('%Y-%m-%dT%H:%M:%SZ')}', "
                    f"'{arr_time.strftime('%Y-%m-%dT%H:%M:%SZ')}', "
                    f"'{status}', CURRENT_TIMESTAMP)"
                )
            lines.append(",\n".join(trip_values) + ";")
            lines.append("")
        
        # Trip Seat Availability (batch insert - this is the largest table)
        lines.append("-- ============================================================")
        lines.append("-- TRIP SEAT AVAILABILITY")
        lines.append("-- ============================================================")
        batch_size = 1000  # Larger batches for this table
        for i in range(0, len(self.trip_seat_availability), batch_size):
            batch = self.trip_seat_availability[i:i + batch_size]
            lines.append("INSERT INTO trip_seat_availability (scheduled_trip_id, seat_id, status, version) VALUES")
            tsa_values = []
            for trip_uuid, seat_uuid in batch:
                tsa_values.append(
                    f"    ('{trip_uuid}', '{seat_uuid}', 'AVAILABLE', 1)"
                )
            lines.append(",\n".join(tsa_values) + ";")
            lines.append("")
        
        return "\n".join(lines)

    @staticmethod
    def _escape(s: str) -> str:
        """Escape single quotes for SQL."""
        return s.replace("'", "''")


# =============================================================================
# Main
# =============================================================================

def main():
    parser = argparse.ArgumentParser(
        description="Generate seed data for train ticket booking system."
    )
    parser.add_argument(
        "--output", "-o",
        type=str,
        default="../../backend/src/main/resources/db/migration/R__seed_dev_data.sql",
        help="Output SQL file path (default: R__seed_dev_data.sql in migrations folder)"
    )
    parser.add_argument(
        "--days", "-d",
        type=int,
        default=30,
        help="Number of days to generate scheduled trips for (default: 30)"
    )
    parser.add_argument(
        "--seed", "-s",
        type=int,
        default=12345,
        help="Random seed for reproducibility (default: 12345)"
    )
    
    args = parser.parse_args()
    
    # Generate data
    generator = DataGenerator(seed=args.seed)
    generator.generate_all(days=args.days)
    
    # Write SQL
    sql = generator.to_sql()
    output_path = Path(args.output)
    
    # Handle relative paths
    if not output_path.is_absolute():
        output_path = Path(__file__).parent / output_path
    
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(sql, encoding="utf-8")
    
    print(f"\nSQL written to: {output_path.resolve()}")
    print(f"File size: {output_path.stat().st_size / 1024:.1f} KB")


if __name__ == "__main__":
    main()
