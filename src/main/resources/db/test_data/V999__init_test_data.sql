-- 1. USUÁRIOS (Senha: teste123)
-- Hash BCrypt: $2a$10$8.uXF3.5qT1S9z9V6O9OReK.uXF3.5qT1S9z9V6O9OReK.uXF3.5q
INSERT INTO users (id, name, email, password, role, email_verified, status, created_at, updated_at) VALUES
                                                                                                        ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Administrador', 'admin@ifride.com', '$2a$10$8.uXF3.5qT1S9z9V6O9OReK.uXF3.5qT1S9z9V6O9OReK.uXF3.5q', 'ADMIN', TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                        ('550e8400-e29b-41d4-a716-446655440000', 'Arthur Dent', 'arthur@ifride.com', '$2a$10$8.uXF3.5qT1S9z9V6O9OReK.uXF3.5qT1S9z9V6O9OReK.uXF3.5q', 'DRIVER', TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                        ('6ba7b810-9dad-11d1-80b4-00c04fd430c8', 'Ford Prefect', 'ford@ifride.com', '$2a$10$8.uXF3.5qT1S9z9V6O9OReK.uXF3.5qT1S9z9V6O9OReK.uXF3.5q', 'DRIVER', TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                        ('3e58498b-7004-44f2-959c-85f26f2f9f1b', 'Tricia McMillan', 'trillian@ifride.com', '$2a$10$8.uXF3.5qT1S9z9V6O9OReK.uXF3.5qT1S9z9V6O9OReK.uXF3.5q', 'PASSENGER', TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                        ('d290f1ee-6c54-4b01-90e6-d701748f0851', 'Zaphod Beeblebrox', 'zaphod@ifride.com', '$2a$10$8.uXF3.5qT1S9z9V6O9OReK.uXF3.5qT1S9z9V6O9OReK.uXF3.5q', 'PASSENGER', TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. SOLICITAÇÕES DE MOTORISTA
INSERT INTO driver_applications (id, user_id, application_status, cnh_number, cnh_category, cnh_expiration, created_at, updated_at) VALUES
                                                                                                                                        ('123e4567-e89b-12d3-a456-426614174002', '550e8400-e29b-41d4-a716-446655440000', 'APPROVED', '123456789', 'B', '2030-12-31', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                        ('123e4567-e89b-12d3-a456-426614174003', '6ba7b810-9dad-11d1-80b4-00c04fd430c8', 'APPROVED', '987654321', 'B', '2028-05-15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. MOTORISTAS (O ID é o mesmo do Usuário conforme sua FK)
INSERT INTO drivers (id, cnh_number, cnh_category, cnh_expiration, created_at, updated_at) VALUES
                                                                                               ('550e8400-e29b-41d4-a716-446655440000', '123456789', 'B', '2030-12-31', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                               ('6ba7b810-9dad-11d1-80b4-00c04fd430c8', '987654321', 'B', '2028-05-15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 4. VEÍCULOS
INSERT INTO vehicles (id, driver_id, model, plate, capacity, color, created_at, updated_at) VALUES
  ('f47ac10b-58cc-4372-a567-0e02b2c3d479', '550e8400-e29b-41d4-a716-446655440000', 'Toyota Corolla', 'IFR-2026', 4, 'Prata', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('7c9e6639-7428-40de-91bb-cf053f4e2424', '6ba7b810-9dad-11d1-80b4-00c04fd430c8', 'Volkswagen Golf', 'FLT-0000', 3, 'Azul', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 5. CARONAS
INSERT INTO rides (id, driver_id, vehicle_id, available_seats, total_seats, origin, destination, departure_time, ride_status, price, created_at, updated_at) VALUES
  ('123e4567-e89b-12d3-a456-426614174000', '550e8400-e29b-41d4-a716-446655440000', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 3, 4, 'Urutaí', 'Orizona', CURRENT_TIMESTAMP + interval '2 hours', 'SCHEDULED', 7.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('123e4567-e89b-12d3-a456-426614174001', '6ba7b810-9dad-11d1-80b4-00c04fd430c8', '7c9e6639-7428-40de-91bb-cf053f4e2424', 0, 3, 'Urutaí', 'Pires do Rio', CURRENT_TIMESTAMP + interval '1 day', 'SCHEDULED', 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 6. PONTOS DE EMBARQUE
INSERT INTO ride_pickup_points (ride_id, point_name, point_order) VALUES
  ('123e4567-e89b-12d3-a456-426614174000', 'Entrada Bloco A', 0),
  ('123e4567-e89b-12d3-a456-426614174000', 'RU', 1),
  ('123e4567-e89b-12d3-a456-426614174001', 'Portão Norte', 0);

-- 7. PARTICIPANTES
INSERT INTO ride_participants (id, ride_id, user_id, participant_status, requested_at, created_at, updated_at) VALUES
  ('30283c48-6923-45f1-8e0c-806733f38131', '123e4567-e89b-12d3-a456-426614174000', '3e58498b-7004-44f2-959c-85f26f2f9f1b', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('4887b47b-166e-47f2-a25e-2f3b9c03b123', '123e4567-e89b-12d3-a456-426614174001', '3e58498b-7004-44f2-959c-85f26f2f9f1b', 'ACCEPTED', CURRENT_TIMESTAMP - interval '1 hour', CURRENT_TIMESTAMP - interval '1 hour', CURRENT_TIMESTAMP);