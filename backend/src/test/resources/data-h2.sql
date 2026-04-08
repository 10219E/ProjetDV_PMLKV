INSERT INTO dbo.Users_Roles (id, name, description) VALUES
    (0, 'invite', 'Membre externe invite'),
    (1, 'subscribed', 'Membre avec souscription a au moins un site'),
    (2, 'all_site', 'Membre VIP - souscription multi-sites'),
    (7, 'site_admin', 'Administrateur site'),
    (9, 'as_admin', 'Super Administrateur');

INSERT INTO dbo.Users (user_id, fname, lname, email, bdate, role_id, lvl) VALUES
    ('A001', 'Paul', 'Mlkv', 'pmlkv@ephec.be', '1995-11-05', 9, NULL),
    ('G0001', 'Marie', 'Chlo', 'mchlo@ephec.be', '1998-07-01', 2, 'débutant'),
    ('S0001', 'Jean', 'Martin', 'jmartin@ephec.be', '1988-03-10', 1, 'averti'),
    ('L0001', 'Sophie', 'Bernard', 'sbernard@ephec.be', '1992-07-25', 0, 'confirmé');

INSERT INTO dbo.Sites (name, address, opening_time, closing_time, is_active) VALUES
    ('Diego Rivera', '72 Rue Pescatore', '08:00:00', '17:00:00', 1),
    ('Da Vinci', '2 Av. Bourget', '08:00:00', '17:00:00', 1);

INSERT INTO dbo.Sites_ClosureDays (site_id, closure_date, reason) VALUES
    (1, '2026-11-11', 'Armistice'),
    (1, '2026-11-11', 'Armistice');

INSERT INTO dbo.Fields (site_id, is_indoor, is_active, maintenance_from_date, maintenance_to_date) VALUES
    (1, 1, 1, NULL, NULL),
    (1, 0, 1, NULL, NULL),
    (1, 1, 1, '2026-07-01', '2026-07-15'),
    (2, 0, 1, NULL, NULL),
    (2, 0, 1, NULL, NULL),
    (2, 1, 1, '2026-09-12', '2026-09-15');

INSERT INTO dbo.Sites_Sessions (site_id, match_sessions_json) VALUES
    (1, '{"sessions":[{"match_set_id":1,"start_time":"08:15:00","end_time":"09:45:00","duration_minutes":90},{"match_set_id":2,"start_time":"10:00:00","end_time":"11:30:00","duration_minutes":90},{"match_set_id":3,"start_time":"11:45:00","end_time":"13:15:00","duration_minutes":90},{"match_set_id":4,"start_time":"13:30:00","end_time":"15:00:00","duration_minutes":90},{"match_set_id":5,"start_time":"15:15:00","end_time":"16:45:00","duration_minutes":90}]}'),
    (2, '{"sessions":[{"match_set_id":1,"start_time":"08:15:00","end_time":"09:45:00","duration_minutes":90},{"match_set_id":2,"start_time":"10:00:00","end_time":"11:30:00","duration_minutes":90},{"match_set_id":3,"start_time":"11:45:00","end_time":"13:15:00","duration_minutes":90},{"match_set_id":4,"start_time":"13:30:00","end_time":"15:00:00","duration_minutes":90},{"match_set_id":5,"start_time":"15:15:00","end_time":"16:45:00","duration_minutes":90}]}');

INSERT INTO dbo.Users_Accounts (user_id, balance, status) VALUES
    ('G0001', 0.00, 'clear'),
    ('S0001', 0.00, 'clear'),
    ('L0001', 0.00, 'clear');

INSERT INTO dbo.Users_Sites (user_id, site_id, is_primary, is_vip) VALUES
    ('G0001', 2, 1, 1),
    ('S0001', 1, 1, 0),
    ('L0001', 2, 1, 0);

