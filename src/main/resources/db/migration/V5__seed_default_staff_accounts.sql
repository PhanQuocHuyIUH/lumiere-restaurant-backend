-- Seed default staff accounts for first bootstrap.
-- Shared default password (raw): password@
-- BCrypt hash for password@: $2a$10$OoR7lWLAtRkLj8KhpIC2VuUkPtNumoPUTleusYt81qYYLY8uuRXjW
-- IMPORTANT: rotate this credential immediately after the first deployment.
INSERT INTO identity.staff (name, role, username, password_hash, status)
VALUES
    (
        'Default Manager',
        'MANAGER',
        'admin',
        '$2a$10$OoR7lWLAtRkLj8KhpIC2VuUkPtNumoPUTleusYt81qYYLY8uuRXjW',
        'ACTIVE'
    ),
    (
        'Waiter 01',
        'WAITER',
        'waiter01',
        '$2a$10$OoR7lWLAtRkLj8KhpIC2VuUkPtNumoPUTleusYt81qYYLY8uuRXjW',
        'ACTIVE'
    ),
    (
        'Waiter 02',
        'WAITER',
        'waiter02',
        '$2a$10$OoR7lWLAtRkLj8KhpIC2VuUkPtNumoPUTleusYt81qYYLY8uuRXjW',
        'ACTIVE'
    ),
    (
        'Waiter 03',
        'WAITER',
        'waiter03',
        '$2a$10$OoR7lWLAtRkLj8KhpIC2VuUkPtNumoPUTleusYt81qYYLY8uuRXjW',
        'ACTIVE'
    ),
    (
        'Waiter 04',
        'WAITER',
        'waiter04',
        '$2a$10$OoR7lWLAtRkLj8KhpIC2VuUkPtNumoPUTleusYt81qYYLY8uuRXjW',
        'ACTIVE'
    ),
    (
        'Cashier 01',
        'CASHIER',
        'cashier01',
        '$2a$10$OoR7lWLAtRkLj8KhpIC2VuUkPtNumoPUTleusYt81qYYLY8uuRXjW',
        'ACTIVE'
    ),
    (
        'Cashier 02',
        'CASHIER',
        'cashier02',
        '$2a$10$OoR7lWLAtRkLj8KhpIC2VuUkPtNumoPUTleusYt81qYYLY8uuRXjW',
        'ACTIVE'
    ),
    (
        'Kitchen 01',
        'KITCHEN',
        'kitchen01',
        '$2a$10$OoR7lWLAtRkLj8KhpIC2VuUkPtNumoPUTleusYt81qYYLY8uuRXjW',
        'ACTIVE'
    )
ON CONFLICT (username) DO NOTHING;