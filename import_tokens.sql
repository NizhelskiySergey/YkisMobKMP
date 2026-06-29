-- Фінальний скрипт для бази YISGRAND (MySQL)
-- Таблиця: FASTPAY_TOKENS

CREATE TABLE IF NOT EXISTS `FASTPAY_TOKENS` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `osbbId` int(11) NOT NULL DEFAULT 0,
    `name` varchar(255) NOT NULL,
    `biplan_id` bigint(20) DEFAULT NULL,
    `okpo` bigint(20) DEFAULT NULL,
    `full_url` text DEFAULT NULL,
    `token` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`, `osbbId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

TRUNCATE TABLE `FASTPAY_TOKENS`;

INSERT INTO `FASTPAY_TOKENS` (`osbbId`, `name`, `biplan_id`, `okpo`, `full_url`, `token`) VALUES
(9999, 'Водопостачання та каналізація, КП м.Південне', 3856164, 31783053, 'https://next.privat24.ua/payments/form/{"token":"eb6ea420aafcfed30f1e8004bad725238xp65yel"}', 'eb6ea420aafcfed30f1e8004bad725238xp65yel'),
(9998, 'КП тм ЮТКЕ, м.Південне', 3857492, 26134519, 'https://next.privat24.ua/payments/form/{"token":"0ab8c0c0ed1d854b49f465c12deb6dfbh4oipwm3"}', '0ab8c0c0ed1d854b49f465c12deb6dfbh4oipwm3'),
(9997, 'КП Спецтранс', 3857464, 30750184, 'https://next.privat24.ua/payments/form/{"token":"5b0dcc38ed7a4fb13823b5176d7f1428h4oipwm3"}', '5b0dcc38ed7a4fb13823b5176d7f1428h4oipwm3'),
(24, 'КК ДОБРОБУТ-СЕРВІС ТОВ', 3855381, 40954168, 'https://next.privat24.ua/payments/form/{"token":"fcc58437616b11a5e6c89412d2985bdde7q9emgv"}', 'fcc58437616b11a5e6c89412d2985bdde7q9emgv'),
(3, 'ОСББ Мирний 26', 3849951, 40970860, 'https://next.privat24.ua/payments/form/{"token":"a518fd69274d874dfa505ea49343a9f7e7q9emgv"}', 'a518fd69274d874dfa505ea49343a9f7e7q9emgv'),
(13, 'Миру 13, ОСББ', 3857062, 43258948, 'https://next.privat24.ua/payments/form/{"token":"d630408099f83ecef2d2805f68e28febe7q9emgv"}', 'd630408099f83ecef2d2805f68e28febe7q9emgv'),
(23, 'Десанту 23, ОСББ', 3857415, 43328350, 'https://next.privat24.ua/payments/form/{"token":"8ffd51bfc95eb69558964c8e44fd4e0de7q9emgv"}', '8ffd51bfc95eb69558964c8e44fd4e0de7q9emgv'),
(20, 'Десанту 20, ОСББ', 3856689, 41130740, 'https://next.privat24.ua/payments/form/{"token":"1f2a32c20556298acf3ed8dea98985e7e7q9emgv"}', '1f2a32c20556298acf3ed8dea98985e7e7q9emgv'),
(21, 'ПРИМОРСЬКА 21, ОСББ', 3857395, 41038177, 'https://next.privat24.ua/payments/form/{"token":"1ff93efd9dd859cdae8155da1ea1ca90e7q9emgv"}', '1ff93efd9dd859cdae8155da1ea1ca90e7q9emgv'),
(7, 'Т.Г.ШЕВЧЕНКО 7, ОСББ', 3857372, 40631936, 'https://next.privat24.ua/payments/form/{"token":"9349811ef4f3f4c00b4dae00d805a773e7q9emgv"}', '9349811ef4f3f4c00b4dae00d805a773e7q9emgv'),
(6, 'ХІМІКІВ 6, ОСББ', 3857448, 41142274, 'https://next.privat24.ua/payments/form/{"token":"74367a7396a2659d0939f666497af2c3e7q9emgv"}', '74367a7396a2659d0939f666497af2c3e7q9emgv'),
(10, 'ХІМІКІВ-10, ОСББ', 3857347, 40980659, 'https://next.privat24.ua/payments/form/{"token":"c68dae795ea218edc38fd4169fa77a0ee7q9emgv"}', 'c68dae795ea218edc38fd4169fa77a0ee7q9emgv'),
(14, 'ХІМІКІВ 14, ОСББ', 3857514, 41122127, 'https://next.privat24.ua/payments/form/{"token":"22eb71fc-73dd-48f0-afc0-7c4f709fb3b8"}', '22eb71fc-73dd-48f0-afc0-7c4f709fb3b8'),
(11, 'Приморська 11, ОСББ', 3867317, 40431141, 'https://next.privat24.ua/payments/form/{"token":"a2b085e77208f46314eaf24da47e2112e7q9emgv"}', 'a2b085e77208f46314eaf24da47e2112e7q9emgv'),
(20, 'Хіміків 20, ОСББ', 3871800, 40980617, 'https://next.privat24.ua/payments/form/{"token":"4f112b478c3c011531ffbf53e42b4108e7q9emgv"}', '4f112b478c3c011531ffbf53e42b4108e7q9emgv'),
(28, 'Рідний дім 28, ОСББ', 3878563, 40624850, 'https://next.privat24.ua/payments/form/{"token":"b2d58cb6fa75083b5f4839ca560e2276e7q9emgv"}', 'b2d58cb6fa75083b5f4839ca560e2276e7q9emgv'),
(2, 'Хіміків 2, ОСББ', 3878120, 41121212, 'https://next.privat24.ua/payments/form/{"token":"1c0d2f57fbd84bcd5849c92a68a6fb44io7ukq9e"}', '1c0d2f57fbd84bcd5849c92a68a6fb44io7ukq9e'),
(12, 'Хіміків 12, ОСББ', 3878138, 40687046, 'https://next.privat24.ua/payments/form/{"token":"289d422a58f1625d30752a7f97eebb37e7q9emgv"}', '289d422a58f1625d30752a7f97eebb37e7q9emgv'),
(5, 'Шевченко 5, ОСББ', 3884415, 41081201, 'https://next.privat24.ua/payments/form/{"token":"c72d8123d334fb44c76ac29db462a21ce7q9emgv"}', 'c72d8123d334fb44c76ac29db462a21ce7q9emgv'),
(19, 'ПРИМОРСЬКА 19, ОСББ', 3947904, 40646104, 'https://next.privat24.ua/payments/form/{"token":"0ca435db7a5de22c1fd8df0f041feccce7q9emgv"}', '0ca435db7a5de22c1fd8df0f041feccce7q9emgv'),
(16, 'Кондомінімум 16, ОСББ', 3959606, 40573843, 'https://next.privat24.ua/payments/form/{"token":"337824a8bf408c01178cb0f4e0b25fede7q9emgv"}', '337824a8bf408c01178cb0f4e0b25fede7q9emgv'),
(18, 'Химиков 18, ОСББ', 4016004, 40702603, 'https://next.privat24.ua/payments/form/{"token":"4d4bfe2c24dabce09a78088d8fd58d04e7q9emgv"}', '4d4bfe2c24dabce09a78088d8fd58d04e7q9emgv'),
(17, 'Миру 17, ОСББ', 4102580, 41231882, 'https://next.privat24.ua/payments/form/{"token":"f232c9ecb44e9f11135128bb39da3037e7q9emgv"}', 'f232c9ecb44e9f11135128bb39da3037e7q9emgv'),
(9, 'Строителей 9, ОСББ', 4123450, 41072396, 'https://next.privat24.ua/payments/form/{"token":"74f81625282c93c8a60c79eb4ac31f0ae7q9emgv"}', '74f81625282c93c8a60c79eb4ac31f0ae7q9emgv'),
(8, 'ХІМІКІВ 8, ОСББ', 4479712, 40618983, 'https://next.privat24.ua/payments/form/{"token":"b5ac6af4b8e862933ee7d38b939ec564e7q9emgv"}', 'b5ac6af4b8e862933ee7d38b939ec564e7q9emgv');
