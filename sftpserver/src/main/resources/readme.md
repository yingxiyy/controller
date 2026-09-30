
#1. User & group
sudo groupadd sftpusers
sudo useradd -m -g sftpusers -s /usr/sbin/nologin dciUser
sudo passwd dciUser

#2. Directory structure
sudo chown root:root /sftp/dciUser
sudo chmod 755 /sftp/dciUser

sudo mkdir -p /sftp/dciUser/upload
sudo chown dciUser:sftpusers /sftp/dciUser/upload
sudo chmod 755 /sftp/dciUser/upload

#3. modify SSHD config  (/etc/ssh/sshd_config)
Match Group sftpusers
    ChrootDirectory /sftp/%u
    ForceCommand internal-sftp
    X11Forwarding no
    AllowTcpForwarding no

#4. Restart
sudo systemctl restart sshd

#5. Test
sftp dciUser@yourserver
	


#---------------------------------
vi /etc/ssh/sshd_sftp.conf
Port 2222
ListenAddress 0.0.0.0

Protocol 2
HostKey /etc/ssh/ssh_host_rsa_key
HostKey /etc/ssh/ssh_host_ed25519_key

Subsystem sftp internal-sftp

PermitRootLogin no
PasswordAuthentication yes
ChallengeResponseAuthentication no
UsePAM yes
X11Forwarding no
AllowTcpForwarding no

Match All
ForceCommand internal-sftp
ChrootDirectory /data/sftp



sudo cp /etc/ssh/sshd_config /etc/ssh/sshd_sftp.conf

sudo vi /etc/systemd/system/sshd-sftp.service
[Unit]
Description=OpenSSH SFTP Server (Port 2222)
After=network.target sshd.service
Requires=network.target

[Service]
Type=simple
ExecStart=/usr/sbin/sshd -D -f /etc/ssh/sshd_sftp.conf -p 2222
ExecReload=/bin/kill -HUP $MAINPID
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=multi-user.target

#################################
sudo systemctl daemon-reload
sudo systemctl start sshd-sftp

sudo systemctl status sshd-sftp