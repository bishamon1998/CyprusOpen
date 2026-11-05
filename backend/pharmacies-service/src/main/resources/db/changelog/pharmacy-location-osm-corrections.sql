--liquibase formatted sql
--changeset cyprus-open:verify-pharmacy-osm-locations-2026-11-05
UPDATE pharmacies.pharmacy_location AS location
SET latitude = verified.latitude,
    longitude = verified.longitude,
    precision = 'exact',
    source = verified.source,
    updated_at = current_timestamp
FROM (VALUES
('380', 35.1429576, 33.3664798, 'https://www.openstreetmap.org/node/266316533'),
('968', 35.154348, 33.3171495, 'https://www.openstreetmap.org/node/4422065691'),
('596', 35.1611125, 33.3748105, 'https://www.openstreetmap.org/node/266317081'),
('1361', 35.1785265, 33.378711, 'https://www.openstreetmap.org/node/13948079401'),
('990', 35.1481543, 33.3194089, 'https://www.openstreetmap.org/node/4422047296'),
('1315', 35.1477583, 33.3541557, 'https://www.openstreetmap.org/node/12083583674'),
('625', 35.1406047, 33.338664, 'https://www.openstreetmap.org/node/294096958'),
('1074', 35.1394636, 33.3678862, 'https://www.openstreetmap.org/node/4429324189'),
('660', 35.1607944, 33.353242, 'https://www.openstreetmap.org/node/3827330472'),
('471', 35.1679325, 33.3716706, 'https://www.openstreetmap.org/node/226972384'),
('654', 35.140957, 33.3112241, 'https://www.openstreetmap.org/node/4422047295'),
('601', 35.1590472, 33.3430795, 'https://www.openstreetmap.org/node/5079904621'),
('841', 35.1423864, 33.3679811, 'https://www.openstreetmap.org/node/4424885489'),
('636', 35.141065, 33.3450938, 'https://www.openstreetmap.org/node/12573871763'),
('574', 35.1590857, 33.368528, 'https://www.openstreetmap.org/node/264588175'),
('785', 35.1827011, 33.3865071, 'https://www.openstreetmap.org/node/266071333'),
('717', 34.6968317, 33.0600343, 'https://www.openstreetmap.org/node/1260930325'),
('962', 34.6735134, 32.92252, 'https://www.openstreetmap.org/node/353174868'),
('1099', 34.6590437, 33.0033226, 'https://www.openstreetmap.org/node/13880827465'),
('384', 34.6785318, 33.024202, 'https://www.openstreetmap.org/node/5977901888'),
('1064', 34.6749105, 33.0128353, 'https://www.openstreetmap.org/node/4434863589'),
('410', 34.6844045, 33.0288175, 'https://www.openstreetmap.org/node/1260937305'),
('963', 34.6954897, 33.0632158, 'https://www.openstreetmap.org/node/1260951912'),
('1054', 34.696421, 33.0890705, 'https://www.openstreetmap.org/node/4902222921'),
('696', 34.7029047, 33.0281532, 'https://www.openstreetmap.org/node/5996560486'),
('718', 34.6823661, 33.0478536, 'https://www.openstreetmap.org/node/11263028001'),
('593', 34.6937082, 33.0014536, 'https://www.openstreetmap.org/node/1260927434'),
('888', 34.6947563, 33.0809461, 'https://www.openstreetmap.org/node/1253690621'),
('933', 34.69226, 33.0353603, 'https://www.openstreetmap.org/node/332718003'),
('975', 34.7820517, 32.4267845, 'https://www.openstreetmap.org/node/4291175289'),
('922', 34.7627123, 32.438634, 'https://www.openstreetmap.org/node/292577394'),
('310', 34.77722, 32.4232058, 'https://www.openstreetmap.org/node/7125038487'),
('912', 34.7865486, 32.437752, 'https://www.openstreetmap.org/node/11505667207')
) AS verified(registration_number, latitude, longitude, source)
WHERE location.registration_number = verified.registration_number
  AND location.source = 'manual review';