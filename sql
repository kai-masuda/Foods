Dbeaverでlocalhost,SQLエディタ用

CREATE DATABASE food_board CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
CREATE USER 'devuser'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON food_board.* to 'devuser'@'localhost';
