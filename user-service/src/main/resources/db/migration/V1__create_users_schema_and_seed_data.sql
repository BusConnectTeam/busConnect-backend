-- ============================================
-- Schema y tabla de usuarios de BusConnect
-- ============================================
-- Incluye usuarios de ejemplo para desarrollo
-- ============================================

-- Crear schema si no existe
CREATE SCHEMA IF NOT EXISTS user_service;

-- Crear tabla de usuarios
CREATE TABLE IF NOT EXISTS user_service.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Índices para búsquedas rápidas
CREATE INDEX IF NOT EXISTS idx_users_email ON user_service.users(email);
CREATE INDEX IF NOT EXISTS idx_users_active ON user_service.users(is_active);
CREATE INDEX IF NOT EXISTS idx_users_role ON user_service.users(role);

-- ============================================
-- USUARIOS DE EJEMPLO
-- ============================================

-- Administrador del sistema
INSERT INTO user_service.users (email, first_name, last_name, phone, role) VALUES
('admin@busconnect.cat', 'Admin', 'BusConnect', '+34 600 000 001', 'ADMIN')
ON CONFLICT (email) DO NOTHING;

-- Usuarios tipo CUSTOMER (clientes)
INSERT INTO user_service.users (email, first_name, last_name, phone, role) VALUES
('joan.garcia@gmail.com', 'Joan', 'García López', '+34 612 345 678', 'CUSTOMER'),
('maria.martinez@outlook.com', 'Maria', 'Martínez Puig', '+34 623 456 789', 'CUSTOMER'),
('pere.sanchez@yahoo.es', 'Pere', 'Sánchez Ferrer', '+34 634 567 890', 'CUSTOMER'),
('anna.font@gmail.com', 'Anna', 'Font Casas', '+34 645 678 901', 'CUSTOMER'),
('jordi.vila@hotmail.com', 'Jordi', 'Vila Roca', NULL, 'CUSTOMER')
ON CONFLICT (email) DO NOTHING;

-- Usuarios tipo COMPANY (empresas de transporte)
INSERT INTO user_service.users (email, first_name, last_name, phone, role) VALUES
('transport@alsa.es', 'Carlos', 'Ruiz Moreno', '+34 902 422 242', 'COMPANY'),
('info@sagales.com', 'Montserrat', 'Sagalés Prat', '+34 938 931 700', 'COMPANY'),
('contacto@moventis.es', 'Francesc', 'Moventis Busquets', '+34 900 100 000', 'COMPANY')
ON CONFLICT (email) DO NOTHING;

-- ============================================
-- Resumen de usuarios de ejemplo:
-- ============================================
-- | Email                          | Rol      | Descripción                    |
-- |--------------------------------|----------|--------------------------------|
-- | admin@busconnect.cat           | ADMIN    | Administrador del sistema      |
-- | joan.garcia@gmail.com          | CUSTOMER | Cliente de Barcelona           |
-- | maria.martinez@outlook.com     | CUSTOMER | Cliente de Girona              |
-- | pere.sanchez@yahoo.es          | CUSTOMER | Cliente de Lleida              |
-- | anna.font@gmail.com            | CUSTOMER | Cliente de Tarragona           |
-- | jordi.vila@hotmail.com         | CUSTOMER | Cliente sin teléfono           |
-- | transport@alsa.es              | COMPANY  | Empresa ALSA                   |
-- | info@sagales.com               | COMPANY  | Empresa Sagalés                |
-- | contacto@moventis.es           | COMPANY  | Empresa Moventis               |
-- ============================================
