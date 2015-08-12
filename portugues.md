# Introdução #

O **WebXpose** é um pequeno servidor web, que permite a você compartilhar arquivos do seu dispositivo Android utilizando HTTP.


# Detalhes #

Ele "escuta" pedidos de conexão na porta selecionada por você, logo, você deve acrescentar ":porta" na URL utilizada para acessar o conteúdo. Por exemplo: http://10.0.2.15:8080.

Ele também necessita abrir uma porta para acesso administrativo. Sugerimos que você sempre utilize os valores default, que são: 8080 e 8081.

Quanto você inicia o servidor, um Serviço nativo Android é criado, em processo separado, e fica aguardando conexões até que você o desative. Você pode desativar o serviço executando novamente o WebXpose e clicando no botão "stop". Ou então, abra "Configurações / Aplicações / Gerenciar serviços" e pare o serviço WebXpose.

Você somente pode expor arquivos que estejam em seu sdcard. Você pode deixar o valor default "/sdcard" ou pode criar uma subpasta dentro do sdcard com os arquivos que deseja compartilhar, o que é mais seguro.

# Usando WebXpose #

WebXpose abre um soquete TCP, para que você possa usá-lo em qualquer rede TCP/IP. Mas existem algumas limitações, por exemplo, não há suporte nativo para redes WiFi AdHoc no Android, de modo a única maneira de compartilhar arquivos usando a conexão WiFi é através de um roteador Wireless. Outra limitação importante sobre conexões 3G, o que, normalmente, não permitem que você abra um socket passivo para o mundo. Embora tenhamos testado em duas das maiores operadoras brasileiras (TIM e VIVO) e funcionou muito bem!

Mas, se ambos os dispositivos estão sob a mesma rede, por exemplo, um laptop e seu tablet (ou smartphone), você pode compartilhar arquivos com qualquer dispositivo que tenha um navegador instalado. Basta dizer-lhes a URL: http:// <seu endereço de ip - mostrado no WebXpose>:<porta que você escolheu>.