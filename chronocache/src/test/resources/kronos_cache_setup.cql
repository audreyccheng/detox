CREATE TABLE cached_keys(
    k text,
    address bigint,
    author bigint,
    cc_xacts bigint,
    country bigint,
    customer bigint,
    item bigint,
    order_line bigint,
    orders bigint,
    shopping_cart bigint,
    shopping_cart_line bigint,
    PRIMARY KEY (k) );

CREATE TABLE relation_versions(
    name text,
    version counter,
    PRIMARY KEY( name ) );
