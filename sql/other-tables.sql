DROP TABLE IF EXISTS employees, stars_in_xml, movies_in_xml, transactions;

CREATE TABLE employees(
    email VARCHAR(50) NOT NULL,
    password VARCHAR(20) NOT NULL,
    fullName VARCHAR(100) NOT NULL,
    PRIMARY KEY (email)
);

CREATE TABLE stars_in_xml(
    starId varchar(10) NOT NULL,
    xmlId varchar(10) NOT NULL,

    FOREIGN KEY (starId) REFERENCES stars(id),
    PRIMARY KEY (xmlId)
);

CREATE TABLE movies_in_xml(
    movieId varchar(10) NOT NULL,
    xmlId varchar(10) NOT NULL,

    FOREIGN KEY (movieId) REFERENCES movies(id),
    PRIMARY KEY (movieId, xmlId)

);

CREATE TABLE transactions(
    transactionId INT NOT NULL,
    saleId INT NOT NULL,
    quantity INT NOT NULL,

    PRIMARY KEY (transactionId, saleId)
);