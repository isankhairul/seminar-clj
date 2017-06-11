# ************************************************************
# Sequel Pro SQL dump
# Version 4541
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 5.6.25)
# Database: seminar_scraping
# Generation Time: 2017-06-11 16:36:50 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table member
# ------------------------------------------------------------

DROP TABLE IF EXISTS `member`;

CREATE TABLE `member` (
  `member_id` int(11) NOT NULL,
  `email` varchar(100) NOT NULL DEFAULT '',
  `firstname` varchar(50) NOT NULL DEFAULT '',
  `lastname` varchar(50) NOT NULL DEFAULT '',
  `gender` varchar(50) DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '0',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modified_date` datetime DEFAULT NULL,
  PRIMARY KEY (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `member` WRITE;
/*!40000 ALTER TABLE `member` DISABLE KEYS */;

INSERT INTO `member` (`member_id`, `email`, `firstname`, `lastname`, `gender`, `dob`, `phone`, `status`, `created_date`, `modified_date`)
VALUES
	(1,'luqman@gmail.com','luqmans','hakim','L','1992-06-16','021',1,'2017-06-10 18:30:50',NULL),
	(3,'rio@gmail.com','rio','josef','L','1990-06-18','0812',1,'2017-06-10 18:30:50',NULL),
	(4,'pentry@gmail.com','pentry','yurhadi','L','1989-08-16','021',1,'2017-06-10 18:30:50',NULL),
	(7,'sandra@gmail.com','sandra','DJ','L','1993-10-20','0812',1,'2017-06-10 18:30:50',NULL),
	(9,'robert@gmail.com','robert','purnama','L','1980-05-12','021',0,'2017-06-11 15:05:28',NULL),
	(10,'isaac@gmail.com','isaac','omy','L','1992-05-04','021',1,'2017-06-11 15:05:28',NULL),
	(16,'haveis@gmail.com','Muhammad','Haveis','L','1989-08-09','112233445566',1,'2017-06-11 23:34:22',NULL);

/*!40000 ALTER TABLE `member` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table seminar
# ------------------------------------------------------------

DROP TABLE IF EXISTS `seminar`;

CREATE TABLE `seminar` (
  `seminar_id` int(11) unsigned NOT NULL,
  `tema` varchar(100) NOT NULL DEFAULT '',
  `jadwal` datetime DEFAULT NULL,
  `tempat` varchar(100) NOT NULL DEFAULT '',
  `pembicara` varchar(100) NOT NULL DEFAULT '',
  `kuota` int(11) NOT NULL,
  `sisa_kuota` int(11) NOT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '0',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modified_date` datetime DEFAULT NULL,
  PRIMARY KEY (`seminar_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `seminar` WRITE;
/*!40000 ALTER TABLE `seminar` DISABLE KEYS */;

INSERT INTO `seminar` (`seminar_id`, `tema`, `jadwal`, `tempat`, `pembicara`, `kuota`, `sisa_kuota`, `status`, `created_date`, `modified_date`)
VALUES
	(1,'Big Data','2017-07-28 08:55:00','Auditorium Mercu Buana','Agus',50,47,1,'2017-06-11 16:39:16','2017-06-11 21:04:51'),
	(2,'IT Security','2017-08-17 08:00:00','Auditorium Mercu Buana','Budi',100,98,1,'2017-06-11 16:39:16',NULL),
	(4,'Mobile Application','2017-07-20 09:00:00','Auditorium Mercu Buana','Harianto',50,49,1,'2017-06-11 16:39:16',NULL),
	(5,'Smart City','2017-06-10 21:50:00','Auditorium Mercu Buana','Ibnu Sina',100,97,1,'2017-06-11 16:39:16',NULL);

/*!40000 ALTER TABLE `seminar` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table order_seminar
# ------------------------------------------------------------

DROP TABLE IF EXISTS `order_seminar`;

CREATE TABLE `order_seminar` (
  `order_id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `seminar_id` int(11) NOT NULL,
  `member_id` int(11) NOT NULL,
  `serial` varchar(150) NOT NULL DEFAULT '',
  `created_date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `order_seminar` WRITE;
/*!40000 ALTER TABLE `order_seminar` DISABLE KEYS */;

INSERT INTO `order_seminar` (`order_id`, `seminar_id`, `member_id`, `serial`, `created_date`)
VALUES
	(2,1,3,'BGDT-0001','2017-06-10 20:34:10'),
	(3,2,1,'TSCRTY-0001','2017-06-10 20:43:55'),
	(4,5,1,'SMRTCTY-0001','2017-06-10 21:33:14'),
	(5,1,1,'BGDT-0002','2017-06-10 21:51:07'),
	(6,5,3,'SMRTCTY-0002','2017-06-10 21:52:33'),
	(7,5,7,'SMRTCTY-0003','2017-06-10 21:53:21'),
	(8,4,3,'MBLPPLCTN-0001','2017-06-11 14:29:19'),
	(9,2,3,'TSCRTY-0002','2017-06-11 15:43:25'),
	(10,1,7,'BGDT-0003','2017-06-11 21:03:54');

/*!40000 ALTER TABLE `order_seminar` ENABLE KEYS */;
UNLOCK TABLES;



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
