-- Loopa : schéma initial.
-- Les tables métier portent tenant_id (= shop.id) : filtrées par Hibernate (@TenantId).
-- La Row Level Security PostgreSQL, en fin de fichier, est prête pour une seconde barrière.

-- ---------------------------------------------------------------- tenancy
create table shop (
    id               uuid primary key,
    handle           varchar(64)  not null unique,
    name             varchar(120) not null,
    platform         varchar(20)  not null check (platform in ('SHOPIFY', 'DEMO')),
    shop_domain      varchar(255) unique,
    access_token_enc text,
    currency         varchar(3)    not null,
    default_locale   varchar(5)   not null default 'fr',
    plan             varchar(20)  not null default 'STARTER',
    primary_color    varchar(7)   not null default '#0E7C6B',
    logo_url         varchar(500),
    welcome_message  varchar(280),
    active           boolean      not null default true,
    created_at       timestamptz  not null,
    uninstalled_at   timestamptz
);

create table shop_user (
    id            uuid primary key,
    shop_id       uuid         not null references shop (id),
    email         varchar(254) not null unique,
    name          varchar(120) not null,
    password_hash varchar(255) not null,
    role          varchar(10)  not null check (role in ('OWNER', 'ADMIN', 'AGENT')),
    active        boolean      not null default true,
    last_login_at timestamptz,
    created_at    timestamptz  not null
);
create index shop_user_shop_idx on shop_user (shop_id);

-- ---------------------------------------------------------------- catalog
create table order_snapshot (
    id              uuid primary key,
    tenant_id       uuid         not null references shop (id),
    external_id     varchar(64)  not null,
    name            varchar(32)  not null,
    name_normalized varchar(32)  not null,
    customer_name   varchar(160),
    customer_email  varchar(254),
    customer_phone  varchar(32),
    currency        varchar(3)    not null,
    total_minor     bigint       not null,
    payment_gateway varchar(80),
    ordered_at      timestamptz  not null,
    fulfilled_at    timestamptz,
    synced_at       timestamptz  not null,
    unique (tenant_id, external_id),
    unique (tenant_id, name_normalized)
);

create table order_line_snapshot (
    id               uuid primary key,
    tenant_id        uuid         not null,
    order_id         uuid         not null references order_snapshot (id) on delete cascade,
    position         int          not null,
    external_line_id varchar(64)  not null,
    product_id       varchar(64),
    variant_id       varchar(64),
    title            varchar(255) not null,
    variant_title    varchar(255),
    sku              varchar(120),
    product_type     varchar(120),
    quantity         int          not null check (quantity > 0),
    unit_price_minor bigint       not null check (unit_price_minor >= 0),
    image_url        varchar(1000)
);
create index order_line_order_idx on order_line_snapshot (order_id);

-- ---------------------------------------------------------------- policy
create table return_policy (
    id                       uuid primary key,
    tenant_id                uuid    not null unique references shop (id),
    window_days              int     not null check (window_days between 1 and 365),
    auto_approve             boolean not null,
    auto_approve_max_minor   bigint  not null,
    allow_refund             boolean not null,
    allow_exchange           boolean not null,
    allow_store_credit       boolean not null,
    store_credit_bonus_minor bigint  not null default 0,
    excluded_product_types   varchar(1000)
);

create table return_reason (
    id             uuid primary key,
    tenant_id      uuid        not null references shop (id),
    code           varchar(40) not null,
    label_fr       varchar(80) not null,
    label_en       varchar(80) not null,
    label_ar       varchar(80) not null,
    requires_photo boolean     not null,
    active         boolean     not null,
    position       int         not null,
    unique (tenant_id, code)
);

-- ---------------------------------------------------------------- storage
create table stored_file (
    id           uuid primary key,
    tenant_id    uuid         not null references shop (id),
    filename     varchar(160) not null,
    content_type varchar(80)  not null,
    size         bigint       not null,
    storage_key  varchar(200) not null,
    created_at   timestamptz  not null
);

-- ---------------------------------------------------------------- portal
create table portal_otp (
    id          uuid primary key,
    tenant_id   uuid         not null references shop (id),
    order_id    uuid         not null references order_snapshot (id),
    contact     varchar(254) not null,
    code_hash   varchar(64)  not null,
    attempts    int          not null default 0,
    created_at  timestamptz  not null,
    expires_at  timestamptz  not null,
    consumed_at timestamptz
);
create index portal_otp_order_idx on portal_otp (order_id, created_at);

-- ---------------------------------------------------------------- returns
create sequence return_number_seq start with 1 increment by 1;

create table return_request (
    id               uuid primary key,
    tenant_id        uuid          not null references shop (id),
    number           varchar(32)   not null unique,
    order_id         uuid          not null references order_snapshot (id),
    order_name       varchar(32),
    customer_name    varchar(160),
    customer_email   varchar(254),
    locale           varchar(5)    not null default 'fr',
    status           varchar(20)   not null,
    resolution_type  varchar(20)   not null,
    customer_note    varchar(1000),
    bonus_minor      bigint        not null default 0,
    rejection_reason varchar(500),
    carrier          varchar(40),
    tracking_number  varchar(64),
    resolution_ref   varchar(255),
    resolution_error varchar(1000),
    created_at       timestamptz   not null,
    approved_at      timestamptz,
    updated_at       timestamptz   not null,
    version          bigint        not null default 0,
    constraint return_request_status_chk check (status in
        ('SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED', 'IN_TRANSIT', 'RECEIVED', 'RESOLVED')),
    constraint return_request_resolution_chk check (resolution_type in ('REFUND', 'EXCHANGE', 'STORE_CREDIT'))
);
create index return_request_tenant_created_idx on return_request (tenant_id, created_at desc);
create index return_request_tenant_status_idx on return_request (tenant_id, status);
create index return_request_order_idx on return_request (order_id);

create table return_line (
    id                  uuid primary key,
    tenant_id           uuid         not null,
    return_request_id   uuid         not null references return_request (id) on delete cascade,
    position            int          not null,
    order_line_id       varchar(64)  not null,
    title               varchar(255),
    variant_title       varchar(255),
    quantity            int          not null check (quantity > 0),
    unit_price_minor    bigint       not null check (unit_price_minor >= 0),
    currency            varchar(3)    not null,
    reason_code         varchar(40)  not null,
    exchange_variant_id varchar(120)
);
create index return_line_request_idx on return_line (return_request_id);

create table return_attachment (
    return_request_id uuid not null references return_request (id) on delete cascade,
    position          int  not null,
    file_id           uuid not null references stored_file (id),
    primary key (return_request_id, position)
);

create table return_event (
    id                uuid primary key,
    tenant_id         uuid          not null,
    return_request_id uuid          not null references return_request (id) on delete cascade,
    type              varchar(30)   not null,
    actor             varchar(160)  not null,
    message           varchar(1000),
    occurred_at       timestamptz   not null
);
create index return_event_request_idx on return_event (return_request_id, occurred_at);

-- ---------------------------------------------------------------- Row Level Security (prête)
-- À activer quand la connexion applicative exécute « set local app.tenant_id = '<uuid>' » à chaque transaction
-- (et qu'un rôle dédié, sans BYPASSRLS, est utilisé par l'application) :
-- alter table return_request enable row level security;
-- create policy tenant_isolation on return_request using (tenant_id = current_setting('app.tenant_id')::uuid);
-- (idem pour order_snapshot, order_line_snapshot, return_line, return_event, return_policy, return_reason,
--  stored_file)
