# Introduction #

The **WebXpose** is a tiny web server that lets you share files from your Android device using HTTP.


# Details #

It "listens" for connection requests on the port selected by you, so you should add ":port" in the URL used to access the content. For example: http://10.0.2.15:8080.

It also needs to open a port for administrative access. We suggest you always use the default values​​, which are 8080 and 8081.

As you start the server, an Android native service is created in a separate process, and keeps awaiting connections until you turn it off. You can disable the service running again WebXpose and clicking the "stop", or, open "Settings / Applications / Manage Services" and stop the service WebXpose.

You can only display files that are on your sdcard. You can leave the default "/sdcard" or you can create a subfolder within the sdcard with the files you want to share, which is safer.

# Using WebXpose #

WebXpose opens a TCP Socket, so you can use it in any TCP/IP network. But this has some limitations, for example, there is no native support for AdHoc WiFi networks in Android, so the only way to share files using WiFi connection is through a Wireless router. Other important limitation is 3G connections, which, normally, does not allow you to open a passive socket to the world. Although we have tested in two of the largest Brazilian operators (TIM and VIVO) and it worked fine!

But, if both devices are under the same network, for example a Laptop and your tablet (or smartphone), you can share files with any device that has a Browser installed. Just tell them to type: http://<your ip address - shown in the WebXpose activity>:<the port you choose>.