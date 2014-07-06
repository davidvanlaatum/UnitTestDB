/*
 Navicat MySQL Data Transfer

 Source Server         : database
 Source Server Type    : MySQL
 Source Server Version : 50538
 Source Host           : database
 Source Database       : unittests

 Target Server Type    : MySQL
 Target Server Version : 50538
 File Encoding         : utf-8

 Date: 07/06/2014 16:32:29 PM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `build_unit_tests`
-- ----------------------------
DROP TABLE IF EXISTS `build_unit_tests`;
CREATE TABLE `build_unit_tests` (
  `build_unit_test_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `unit_test_id` int(10) unsigned NOT NULL,
  `job_id` int(10) unsigned NOT NULL,
  `build_id` int(10) unsigned NOT NULL,
  `duration` double unsigned DEFAULT NULL,
  `errordetails` text,
  `errorstack` text,
  `state` enum('Passed','Failed','Skipped','Error') NOT NULL DEFAULT 'Failed',
  `node_id` int(10) unsigned DEFAULT NULL,
  `executor` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`build_unit_test_id`),
  UNIQUE KEY `unit_test_id` (`unit_test_id`,`build_id`),
  KEY `build_id` (`build_id`),
  KEY `job_id` (`job_id`),
  KEY `node_id` (`node_id`),
  CONSTRAINT `build_unit_tests_ibfk_1` FOREIGN KEY (`unit_test_id`) REFERENCES `unit_tests` (`unit_test_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `build_unit_tests_ibfk_2` FOREIGN KEY (`build_id`) REFERENCES `builds` (`build_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `build_unit_tests_ibfk_3` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`job_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `build_unit_tests_ibfk_4` FOREIGN KEY (`node_id`) REFERENCES `nodes` (`node_id`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=latin1;

-- ----------------------------
--  Table structure for `builds`
-- ----------------------------
DROP TABLE IF EXISTS `builds`;
CREATE TABLE `builds` (
  `build_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `job_id` int(10) unsigned NOT NULL,
  `jenkins_id` int(11) NOT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tests` int(10) unsigned DEFAULT NULL,
  `failures` int(10) unsigned DEFAULT '0',
  `skipped` int(11) DEFAULT NULL,
  PRIMARY KEY (`build_id`),
  KEY `job_id` (`job_id`),
  KEY `build_id` (`build_id`),
  KEY `time` (`time`),
  CONSTRAINT `job_id` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`job_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=latin1;

-- ----------------------------
--  Table structure for `failure_users`
-- ----------------------------
DROP TABLE IF EXISTS `failure_users`;
CREATE TABLE `failure_users` (
  `failure_user_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `failure_id` int(10) unsigned NOT NULL,
  `user_id` int(10) unsigned NOT NULL,
  `state` enum('Maybe','Not Me','Was Me','Might be Me') NOT NULL DEFAULT 'Maybe',
  PRIMARY KEY (`failure_user_id`),
  UNIQUE KEY `failure_id` (`failure_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `failure_users_ibfk_1` FOREIGN KEY (`failure_id`) REFERENCES `failures` (`failure_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `failure_users_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=latin1;

-- ----------------------------
--  Table structure for `failures`
-- ----------------------------
DROP TABLE IF EXISTS `failures`;
CREATE TABLE `failures` (
  `failure_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `unit_test_id` int(10) unsigned NOT NULL,
  `job_id` int(10) unsigned NOT NULL,
  `first_build` int(10) unsigned NOT NULL,
  `last_build` int(10) unsigned NOT NULL,
  `state` enum('Failed','Fixed') NOT NULL DEFAULT 'Failed',
  PRIMARY KEY (`failure_id`),
  UNIQUE KEY `unit_test_id` (`unit_test_id`,`job_id`),
  KEY `first_build` (`first_build`),
  KEY `last_build` (`last_build`),
  KEY `job_id` (`job_id`),
  CONSTRAINT `failures_ibfk_1` FOREIGN KEY (`unit_test_id`) REFERENCES `unit_tests` (`unit_test_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `failures_ibfk_3` FOREIGN KEY (`first_build`) REFERENCES `builds` (`build_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `failures_ibfk_4` FOREIGN KEY (`last_build`) REFERENCES `builds` (`build_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `failures_ibfk_5` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`job_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=latin1;

-- ----------------------------
--  Table structure for `jobs`
-- ----------------------------
DROP TABLE IF EXISTS `jobs`;
CREATE TABLE `jobs` (
  `job_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL,
  `lastrun` timestamp NULL DEFAULT NULL,
  `last_build_id` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`job_id`),
  UNIQUE KEY `name` (`name`) USING BTREE,
  KEY `lastrun` (`lastrun`),
  KEY `last_build_id` (`last_build_id`),
  CONSTRAINT `jobs_ibfk_1` FOREIGN KEY (`last_build_id`) REFERENCES `builds` (`build_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=277 DEFAULT CHARSET=latin1;

-- ----------------------------
--  Table structure for `nodes`
-- ----------------------------
DROP TABLE IF EXISTS `nodes`;
CREATE TABLE `nodes` (
  `node_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`node_id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=latin1;

-- ----------------------------
--  Table structure for `unit_tests`
-- ----------------------------
DROP TABLE IF EXISTS `unit_tests`;
CREATE TABLE `unit_tests` (
  `unit_test_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `job_id` int(10) unsigned NOT NULL,
  `name` varchar(512) NOT NULL,
  `failure_rate` double unsigned DEFAULT NULL,
  PRIMARY KEY (`unit_test_id`),
  UNIQUE KEY `name_job` (`name`,`job_id`) USING BTREE,
  KEY `job_id` (`job_id`),
  KEY `unit_test_id` (`unit_test_id`,`job_id`),
  CONSTRAINT `unit_tests_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`job_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=latin1;

-- ----------------------------
--  Table structure for `users`
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `user_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(32) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1;

SET FOREIGN_KEY_CHECKS = 1;
