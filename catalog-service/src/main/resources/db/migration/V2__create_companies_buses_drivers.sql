-- ============================================
-- Empresas de autobuses, tipos de buses y conductores
-- ============================================
-- Empresas de transporte de Catalunya con sus flotas
-- y equipos de conductores
-- ============================================

-- ============================================
-- TABLA: EMPRESAS DE AUTOBUSES
-- ============================================
CREATE TABLE IF NOT EXISTS catalog.bus_companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    legal_name VARCHAR(150) NOT NULL,
    cif VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(200),
    city VARCHAR(100),
    postal_code VARCHAR(10),
    website VARCHAR(100),
    logo_url VARCHAR(255),
    founded_year INTEGER,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bus_companies_name ON catalog.bus_companies(name);
CREATE INDEX IF NOT EXISTS idx_bus_companies_active ON catalog.bus_companies(is_active);

-- ============================================
-- TABLA: TIPOS DE AUTOBUSES
-- ============================================
CREATE TABLE IF NOT EXISTS catalog.bus_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES catalog.bus_companies(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    capacity INTEGER NOT NULL,
    has_wifi BOOLEAN DEFAULT false,
    has_ac BOOLEAN DEFAULT true,
    has_usb_chargers BOOLEAN DEFAULT false,
    has_toilet BOOLEAN DEFAULT false,
    has_wheelchair_access BOOLEAN DEFAULT false,
    has_luggage_compartment BOOLEAN DEFAULT true,
    has_entertainment_system BOOLEAN DEFAULT false,
    seat_type VARCHAR(50) DEFAULT 'standard', -- standard, premium, vip
    description TEXT,
    price_per_km DECIMAL(10, 4),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bus_types_company ON catalog.bus_types(company_id);
CREATE INDEX IF NOT EXISTS idx_bus_types_capacity ON catalog.bus_types(capacity);
CREATE INDEX IF NOT EXISTS idx_bus_types_active ON catalog.bus_types(is_active);

-- ============================================
-- TABLA: CONDUCTORES
-- ============================================
CREATE TABLE IF NOT EXISTS catalog.drivers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES catalog.bus_companies(id) ON DELETE CASCADE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    dni VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    birth_date DATE NOT NULL,
    hire_date DATE NOT NULL,
    license_number VARCHAR(30) NOT NULL,
    license_expiry_date DATE NOT NULL,
    license_type VARCHAR(10) NOT NULL, -- D, D+E, etc.
    years_experience INTEGER DEFAULT 0,
    languages VARCHAR(100), -- es, ca, en, fr
    photo_url VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_drivers_company ON catalog.drivers(company_id);
CREATE INDEX IF NOT EXISTS idx_drivers_active ON catalog.drivers(is_active);
CREATE INDEX IF NOT EXISTS idx_drivers_license_expiry ON catalog.drivers(license_expiry_date);

-- ============================================
-- DATOS: EMPRESAS DE AUTOBUSES (8 empresas)
-- ============================================
INSERT INTO catalog.bus_companies (name, legal_name, cif, email, phone, address, city, postal_code, website, founded_year) VALUES
-- 1. Autocares Barcelona Premium
('Autocares Barcelona Premium', 'Autocares Barcelona Premium S.L.', 'B12345678', 'info@barcelonapremium.cat', '+34 932 123 456', 'Carrer de la Industria, 45', 'Barcelona', '08025', 'www.barcelonapremium.cat', 2005),

-- 2. Costa Brava Tours
('Costa Brava Tours', 'Costa Brava Tours S.A.', 'A23456789', 'reservas@costabratours.com', '+34 972 345 678', 'Avinguda del Mar, 128', 'Lloret de Mar', '17310', 'www.costabratours.com', 1998),

-- 3. Transports Modernos SA
('Transports Modernos', 'Transports Modernos S.A.', 'A34567890', 'contacto@transportsmodernos.es', '+34 937 456 789', 'Poligon Industrial Can Roca, 12', 'Terrassa', '08225', 'www.transportsmodernos.es', 2010),

-- 4. Autocares Catalunya Express
('Catalunya Express', 'Autocares Catalunya Express S.L.', 'B45678901', 'info@catalunyaexpress.cat', '+34 977 567 890', 'Carrer del Port, 89', 'Tarragona', '43004', 'www.catalunyaexpress.cat', 2015),

-- 5. Pirineus Bus
('Pirineus Bus', 'Pirineus Bus Societat Cooperativa', 'F56789012', 'reserves@pirineusbus.cat', '+34 973 678 901', 'Avinguda de les Valls, 34', 'La Seu d''Urgell', '25700', 'www.pirineusbus.cat', 2008),

-- 6. Girona Viatges
('Girona Viatges', 'Girona Viatges i Autocars S.L.', 'B67890123', 'hola@gironaviatges.com', '+34 972 789 012', 'Placa Catalunya, 15', 'Girona', '17001', 'www.gironaviatges.com', 1985),

-- 7. Delta Ebre Transport
('Delta Ebre Transport', 'Delta Ebre Transport S.L.', 'B78901234', 'info@deltaebre.cat', '+34 977 890 123', 'Carrer Major, 56', 'Amposta', '43870', 'www.deltaebre.cat', 2012),

-- 8. Autocars Metropolitans
('Autocars Metropolitans', 'Autocars Metropolitans del Valles S.A.', 'A89012345', 'reserves@autocarsmetropolitans.cat', '+34 935 901 234', 'Carrer de la Riera, 78', 'Sabadell', '08201', 'www.autocarsmetropolitans.cat', 1995);

-- ============================================
-- DATOS: TIPOS DE AUTOBUSES (3 tipos por empresa)
-- ============================================

-- Autocares Barcelona Premium
INSERT INTO catalog.bus_types (company_id, name, capacity, has_wifi, has_ac, has_usb_chargers, has_toilet, has_wheelchair_access, has_luggage_compartment, has_entertainment_system, seat_type, description, price_per_km) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'B12345678'), 'Minibus Ejecutivo', 8, true, true, true, false, false, true, true, 'vip', 'Minibus de lujo para grupos reducidos con asientos de piel', 2.50),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B12345678'), 'Autocar Confort', 35, true, true, true, true, true, true, true, 'premium', 'Autocar de gama alta con todas las comodidades', 1.80),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B12345678'), 'Gran Turismo Premium', 55, true, true, true, true, true, true, true, 'premium', 'Autocar de dos pisos para grandes grupos', 1.50);

-- Costa Brava Tours
INSERT INTO catalog.bus_types (company_id, name, capacity, has_wifi, has_ac, has_usb_chargers, has_toilet, has_wheelchair_access, has_luggage_compartment, has_entertainment_system, seat_type, description, price_per_km) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'A23456789'), 'Minibus Costa', 12, true, true, true, false, false, true, false, 'standard', 'Ideal para excursiones por la Costa Brava', 2.20),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A23456789'), 'Autocar Turistic', 45, true, true, true, true, false, true, true, 'standard', 'Perfecto para rutas turisticas', 1.60),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A23456789'), 'Bus Panoramic', 50, true, true, true, true, true, true, true, 'premium', 'Ventanas panoramicas para disfrutar del paisaje', 1.75);

-- Transports Modernos SA
INSERT INTO catalog.bus_types (company_id, name, capacity, has_wifi, has_ac, has_usb_chargers, has_toilet, has_wheelchair_access, has_luggage_compartment, has_entertainment_system, seat_type, description, price_per_km) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'A34567890'), 'Shuttle Empresarial', 8, true, true, true, false, false, true, false, 'premium', 'Servicio shuttle para empresas', 2.80),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A34567890'), 'Autocar Moderno', 40, true, true, true, true, true, true, true, 'standard', 'Flota renovada con vehiculos de ultima generacion', 1.55),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A34567890'), 'Megabus Ecologico', 60, true, true, true, true, true, true, true, 'standard', 'Autobus hibrido de bajo consumo', 1.40);

-- Catalunya Express
INSERT INTO catalog.bus_types (company_id, name, capacity, has_wifi, has_ac, has_usb_chargers, has_toilet, has_wheelchair_access, has_luggage_compartment, has_entertainment_system, seat_type, description, price_per_km) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'B45678901'), 'Express Mini', 9, true, true, true, false, false, true, false, 'standard', 'Rapido y comodo para grupos pequenos', 2.30),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B45678901'), 'Express Confort', 30, true, true, true, false, true, true, true, 'premium', 'Servicio express con maxima comodidad', 1.90),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B45678901'), 'Express Plus', 52, true, true, true, true, true, true, true, 'premium', 'El mas completo para largas distancias', 1.65);

-- Pirineus Bus
INSERT INTO catalog.bus_types (company_id, name, capacity, has_wifi, has_ac, has_usb_chargers, has_toilet, has_wheelchair_access, has_luggage_compartment, has_entertainment_system, seat_type, description, price_per_km) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'F56789012'), 'Minibus Muntanya', 8, false, true, true, false, false, true, false, 'standard', 'Preparado para rutas de montana', 2.60),
((SELECT id FROM catalog.bus_companies WHERE cif = 'F56789012'), 'Autocar Pirinenc', 38, true, true, true, true, false, true, false, 'standard', 'Adaptado a las carreteras de montana', 1.70),
((SELECT id FROM catalog.bus_companies WHERE cif = 'F56789012'), 'Neu Express', 48, true, true, true, true, true, true, true, 'premium', 'Especial para estaciones de esqui', 1.85);

-- Girona Viatges
INSERT INTO catalog.bus_types (company_id, name, capacity, has_wifi, has_ac, has_usb_chargers, has_toilet, has_wheelchair_access, has_luggage_compartment, has_entertainment_system, seat_type, description, price_per_km) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'B67890123'), 'Minibus Gironi', 10, true, true, true, false, false, true, false, 'standard', 'Perfecto para visitar pueblos medievales', 2.15),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B67890123'), 'Autocar Classic', 42, true, true, true, true, true, true, false, 'standard', 'Experiencia y fiabilidad desde 1985', 1.50),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B67890123'), 'Gran Viatge', 55, true, true, true, true, true, true, true, 'premium', 'Para viajes internacionales', 1.60);

-- Delta Ebre Transport
INSERT INTO catalog.bus_types (company_id, name, capacity, has_wifi, has_ac, has_usb_chargers, has_toilet, has_wheelchair_access, has_luggage_compartment, has_entertainment_system, seat_type, description, price_per_km) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'B78901234'), 'Mini Delta', 8, true, true, true, false, false, true, false, 'standard', 'Ideal para rutas por el Delta del Ebro', 2.40),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B78901234'), 'Ebre Turistic', 32, true, true, true, false, true, true, true, 'standard', 'Excursiones por las Terres de l''Ebre', 1.65),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B78901234'), 'Delta Gran Turisme', 50, true, true, true, true, true, true, true, 'premium', 'Comodidad total para grupos grandes', 1.55);

-- Autocars Metropolitans
INSERT INTO catalog.bus_types (company_id, name, capacity, has_wifi, has_ac, has_usb_chargers, has_toilet, has_wheelchair_access, has_luggage_compartment, has_entertainment_system, seat_type, description, price_per_km) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'A89012345'), 'Shuttle Metro', 12, true, true, true, false, true, true, false, 'standard', 'Conexiones metropolitanas rapidas', 2.00),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A89012345'), 'Interurba Confort', 45, true, true, true, true, true, true, true, 'standard', 'Servicio regular interurbano', 1.45),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A89012345'), 'Metro Premium', 54, true, true, true, true, true, true, true, 'vip', 'Servicio premium para el area metropolitana', 1.70);

-- ============================================
-- DATOS: CONDUCTORES (4 por empresa = 32 total)
-- ============================================

-- Autocares Barcelona Premium (4 conductores)
INSERT INTO catalog.drivers (company_id, first_name, last_name, dni, email, phone, birth_date, hire_date, license_number, license_expiry_date, license_type, years_experience, languages) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'B12345678'), 'Marc', 'Fernandez Vila', '39123456A', 'marc.fernandez@barcelonapremium.cat', '+34 612 001 001', '1978-03-15', '2010-06-01', 'BCN-D-78001', '2028-03-15', 'D+E', 18, 'es,ca,en'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B12345678'), 'Laura', 'Martinez Soler', '39234567B', 'laura.martinez@barcelonapremium.cat', '+34 612 001 002', '1985-07-22', '2015-02-15', 'BCN-D-85002', '2027-07-22', 'D', 12, 'es,ca,fr'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B12345678'), 'Jordi', 'Puig Casals', '39345678C', 'jordi.puig@barcelonapremium.cat', '+34 612 001 003', '1982-11-08', '2012-09-01', 'BCN-D-82003', '2026-11-08', 'D+E', 15, 'es,ca,en,de'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B12345678'), 'Marta', 'Roca Domenech', '39456789D', 'marta.roca@barcelonapremium.cat', '+34 612 001 004', '1990-04-30', '2018-03-20', 'BCN-D-90004', '2029-04-30', 'D', 8, 'es,ca');

-- Costa Brava Tours (4 conductores)
INSERT INTO catalog.drivers (company_id, first_name, last_name, dni, email, phone, birth_date, hire_date, license_number, license_expiry_date, license_type, years_experience, languages) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'A23456789'), 'Xavier', 'Costa Bosch', '40123456E', 'xavier.costa@costabratours.com', '+34 612 002 001', '1975-09-12', '2005-04-10', 'GIR-D-75001', '2027-09-12', 'D+E', 22, 'es,ca,en,fr'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A23456789'), 'Anna', 'Planas Giralt', '40234567F', 'anna.planas@costabratours.com', '+34 612 002 002', '1988-01-25', '2014-07-01', 'GIR-D-88002', '2028-01-25', 'D', 13, 'es,ca,en'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A23456789'), 'Pere', 'Sala Vidal', '40345678G', 'pere.sala@costabratours.com', '+34 612 002 003', '1980-06-18', '2008-11-15', 'GIR-D-80003', '2026-06-18', 'D+E', 19, 'es,ca,fr,it'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A23456789'), 'Nuria', 'Font Marti', '40456789H', 'nuria.font@costabratours.com', '+34 612 002 004', '1992-12-03', '2019-05-20', 'GIR-D-92004', '2029-12-03', 'D', 7, 'es,ca,en');

-- Transports Modernos SA (4 conductores)
INSERT INTO catalog.drivers (company_id, first_name, last_name, dni, email, phone, birth_date, hire_date, license_number, license_expiry_date, license_type, years_experience, languages) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'A34567890'), 'Albert', 'Garcia Romero', '41123456I', 'albert.garcia@transportsmodernos.es', '+34 612 003 001', '1983-02-28', '2012-01-15', 'TRS-D-83001', '2027-02-28', 'D+E', 14, 'es,ca'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A34567890'), 'Sandra', 'Lopez Navarro', '41234567J', 'sandra.lopez@transportsmodernos.es', '+34 612 003 002', '1987-08-14', '2015-06-01', 'TRS-D-87002', '2028-08-14', 'D', 11, 'es,ca,en'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A34567890'), 'David', 'Torres Prat', '41345678K', 'david.torres@transportsmodernos.es', '+34 612 003 003', '1979-05-07', '2011-03-10', 'TRS-D-79003', '2026-05-07', 'D+E', 16, 'es,ca'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A34567890'), 'Cristina', 'Sanchez Ferrer', '41456789L', 'cristina.sanchez@transportsmodernos.es', '+34 612 003 004', '1991-10-21', '2017-09-01', 'TRS-D-91004', '2029-10-21', 'D', 9, 'es,ca,en,pt');

-- Catalunya Express (4 conductores)
INSERT INTO catalog.drivers (company_id, first_name, last_name, dni, email, phone, birth_date, hire_date, license_number, license_expiry_date, license_type, years_experience, languages) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'B45678901'), 'Francesc', 'Valls Segura', '42123456M', 'francesc.valls@catalunyaexpress.cat', '+34 612 004 001', '1981-07-09', '2016-02-01', 'TGN-D-81001', '2027-07-09', 'D+E', 13, 'es,ca'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B45678901'), 'Elena', 'Mora Castell', '42234567N', 'elena.mora@catalunyaexpress.cat', '+34 612 004 002', '1989-03-17', '2018-04-15', 'TGN-D-89002', '2028-03-17', 'D', 10, 'es,ca,en'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B45678901'), 'Ramon', 'Llopis Badia', '42345678O', 'ramon.llopis@catalunyaexpress.cat', '+34 612 004 003', '1976-11-30', '2016-01-10', 'TGN-D-76003', '2026-11-30', 'D+E', 20, 'es,ca,fr'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B45678901'), 'Isabel', 'Ribes Palau', '42456789P', 'isabel.ribes@catalunyaexpress.cat', '+34 612 004 004', '1993-09-05', '2020-06-01', 'TGN-D-93004', '2030-09-05', 'D', 6, 'es,ca,en');

-- Pirineus Bus (4 conductores)
INSERT INTO catalog.drivers (company_id, first_name, last_name, dni, email, phone, birth_date, hire_date, license_number, license_expiry_date, license_type, years_experience, languages) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'F56789012'), 'Oriol', 'Pujol Armengol', '43123456Q', 'oriol.pujol@pirineusbus.cat', '+34 612 005 001', '1977-04-23', '2009-05-01', 'LLE-D-77001', '2027-04-23', 'D+E', 20, 'es,ca,fr'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'F56789012'), 'Mireia', 'Serra Viladomat', '43234567R', 'mireia.serra@pirineusbus.cat', '+34 612 005 002', '1986-12-11', '2014-10-15', 'LLE-D-86002', '2028-12-11', 'D', 12, 'es,ca,fr'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'F56789012'), 'Josep', 'Fabrega Ros', '43345678S', 'josep.fabrega@pirineusbus.cat', '+34 612 005 003', '1984-08-02', '2012-03-20', 'LLE-D-84003', '2026-08-02', 'D+E', 15, 'es,ca'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'F56789012'), 'Carla', 'Aubert Nogue', '43456789T', 'carla.aubert@pirineusbus.cat', '+34 612 005 004', '1994-06-27', '2021-02-01', 'LLE-D-94004', '2030-06-27', 'D', 5, 'es,ca,fr,en');

-- Girona Viatges (4 conductores)
INSERT INTO catalog.drivers (company_id, first_name, last_name, dni, email, phone, birth_date, hire_date, license_number, license_expiry_date, license_type, years_experience, languages) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'B67890123'), 'Enric', 'Calm Pages', '44123456U', 'enric.calm@gironaviatges.com', '+34 612 006 001', '1972-01-19', '1998-06-01', 'GIR-D-72001', '2027-01-19', 'D+E', 28, 'es,ca,en,fr,de'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B67890123'), 'Montse', 'Guell Ribas', '44234567V', 'montse.guell@gironaviatges.com', '+34 612 006 002', '1985-05-08', '2010-09-15', 'GIR-D-85002', '2028-05-08', 'D', 16, 'es,ca,en'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B67890123'), 'Lluis', 'Dalmau Comas', '44345678W', 'lluis.dalmau@gironaviatges.com', '+34 612 006 003', '1980-10-14', '2005-04-01', 'GIR-D-80003', '2026-10-14', 'D+E', 21, 'es,ca,fr,it'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B67890123'), 'Gemma', 'Puigvert Soler', '44456789X', 'gemma.puigvert@gironaviatges.com', '+34 612 006 004', '1991-02-22', '2016-07-10', 'GIR-D-91004', '2029-02-22', 'D', 10, 'es,ca,en');

-- Delta Ebre Transport (4 conductores)
INSERT INTO catalog.drivers (company_id, first_name, last_name, dni, email, phone, birth_date, hire_date, license_number, license_expiry_date, license_type, years_experience, languages) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'B78901234'), 'Vicent', 'Beltran Querol', '45123456Y', 'vicent.beltran@deltaebre.cat', '+34 612 007 001', '1979-07-31', '2013-01-15', 'TGN-D-79001', '2027-07-31', 'D+E', 15, 'es,ca'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B78901234'), 'Rosa', 'Monfort Vidal', '45234567Z', 'rosa.monfort@deltaebre.cat', '+34 612 007 002', '1988-04-12', '2016-05-01', 'TGN-D-88002', '2028-04-12', 'D', 10, 'es,ca,en'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B78901234'), 'Pau', 'Esteve Mestre', '45345678A', 'pau.esteve@deltaebre.cat', '+34 612 007 003', '1983-09-25', '2014-08-20', 'TGN-D-83003', '2026-09-25', 'D+E', 13, 'es,ca'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'B78901234'), 'Teresa', 'Roig Batalla', '45456789B', 'teresa.roig@deltaebre.cat', '+34 612 007 004', '1995-01-08', '2022-03-01', 'TGN-D-95004', '2031-01-08', 'D', 4, 'es,ca,en');

-- Autocars Metropolitans (4 conductores)
INSERT INTO catalog.drivers (company_id, first_name, last_name, dni, email, phone, birth_date, hire_date, license_number, license_expiry_date, license_type, years_experience, languages) VALUES
((SELECT id FROM catalog.bus_companies WHERE cif = 'A89012345'), 'Sergi', 'Mas Carbonell', '46123456C', 'sergi.mas@autocarsmetropolitans.cat', '+34 612 008 001', '1976-06-04', '2000-02-01', 'BCN-D-76001', '2027-06-04', 'D+E', 26, 'es,ca,en'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A89012345'), 'Patricia', 'Soler Fontanals', '46234567D', 'patricia.soler@autocarsmetropolitans.cat', '+34 612 008 002', '1987-11-18', '2012-06-15', 'BCN-D-87002', '2028-11-18', 'D', 14, 'es,ca,en,fr'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A89012345'), 'Miquel', 'Bonet Creus', '46345678E', 'miquel.bonet@autocarsmetropolitans.cat', '+34 612 008 003', '1982-03-09', '2008-10-01', 'BCN-D-82003', '2026-03-09', 'D+E', 18, 'es,ca'),
((SELECT id FROM catalog.bus_companies WHERE cif = 'A89012345'), 'Laia', 'Camps Ventura', '46456789F', 'laia.camps@autocarsmetropolitans.cat', '+34 612 008 004', '1993-08-15', '2019-01-20', 'BCN-D-93004', '2029-08-15', 'D', 7, 'es,ca,en,de');

-- ============================================
-- Resumen de datos creados:
-- ============================================
-- | Tabla          | Registros | Descripcion                    |
-- |----------------|-----------|--------------------------------|
-- | bus_companies  | 8         | Empresas de autobuses          |
-- | bus_types      | 24        | Tipos de buses (3 por empresa) |
-- | drivers        | 32        | Conductores (4 por empresa)    |
-- ============================================
