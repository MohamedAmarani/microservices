# microservices

## Descripción de mis microservicios

### product-service
Este microservicio contiene toda la lógica relacionada con la gestión de los productos de la tienda electrónica. Concretamente, las peticiones HTTP que acepta este microservicio son las relativas a la consulta de todos (pudiendo filtrar y ordenar por parámetros) o alguno de los productos de una tienda electrónica, la creación o eliminación de un producto, a la actualización de la descripción, del color, de la talla, del tipo y del sexo de un producto determinado, editar el precio de un producto, aumentar el número de ventas de un producto (cuando se realice y pague un pedido sobre este), y actualizar la puntuación media obtenida de las reviews (reseñas) que los usuarios den a un producto.
Un producto está formado por un id (único), un nombre (único), una descripción, un color,  una lista de imágenes (pictures), donde cada elemento corresponde a la resourceId de la imagen en cuestión, las cuales gestiono mediante mi microservicio resource-service y guardo en mi base de datos, es decir, no dependo de servicios externos para almacenar mis recursos gráficos, el precio original el producto (originalPrice), este es el precio inicial con el que el producto se creó, el precio actual (currentPrice), el cual representa el precio actual de un producto, que puede ser diferente al original si se ha rebajado o se la ha aplicado algún tipo de promoción, la talla (size) del producto, el tipo (type) del producto, el sexo (sex) del producto, el número total de ventas del producto, el número de reseñas (reviews) del producto), la puntuación media obtenida de las reseñas (reviews) que los usuarios han dado al producto, y la fecha de creación (creationDate).
Todas las peticiones HTTP que acepta este microservicio las tengo definidas en la sección “product-service” del documento Swagger https://app.swaggerhub.com/apis-docs/scaredstew/eCommerceSaaS.
El código al completo de este microservicio, igual que el del resto de microservicios, se puede encontrar en mi repositorio GitHub https://github.com/MohamedAmarani/microservices.

### catalog-service
Este microservicio se encarga de gestionar la lógica de los distintos catálogos que puede tener una tienda electrónica. He creado este microservicio con el objetivo de que mis clientes puedan tener múltiples catálogos activos en su respectiva tienda electrónica. Este es un aspecto clave para asegurar la conectividad de mis clientes en el mercado, ya que una de las características más comunes en las tiendas electrónicas punteras es la de disponer de diferentes catálogos, como, por ejemplo, uno con la colección otoño-invierno y otro con colaboración con otras marcas, disponibles en el mismo momento.
Concretamente, las peticiones HTTP que acepta este microservicio son obtener todos (pudiendo filtrar y ordenar por parámetros) o uno de los catálogos de una tienda electrónica, crear o eliminar un catálogo, añadir un producto a un catálogo determinado y obtener un producto específico de un catálogo especifico.
Un catálogo (catalog) está formado por un id (único), una lista de ítems del catálogo (catalogItems) y la fecha de creación (creationDate). Cada catalogItem, por su parte, está compuesto por el identificador del producto al que hace referencia (productId) y de su fecha de creación (creationDate).

### inventory-service
Este microservicio lo he creado para que se encargue de manejar la lógica relativa a un inventario, es decir, a la gestión del stock, el incremento y la reducción de este, entre otros aspectos. Este microservicio también lo he creado teniendo en mente que en una tienda electrónica sería ideal contar con más de un inventario al mismo tiempo, cada uno, por ejemplo, destinado a un país o zona geográfica diferente, o simplemente a tiendas físicas distintas. Esta característica es clave en un eCommerce, y la he implementado con pleno detalle.
Concretamente, este microservicio, a través de llamadas HTTP, ofrece las funcionalidades de obtener todos (pudiendo filtrar y ordenar por parámetros) o uno de los inventarios de una tienda electrónica, crear y eliminar un determinado inventario, obtener un producto concreto de un inventario concreto, añadir un ítem (producto + cantidad de unidades del producto) a un inventario e incrementar y reducir stock de un determinado producto de un inventario.
Un inventario (inventory) está formado por un id (único), una lista de ítems del inventario (inventoryItems) y la fecha de creación (creationDate). Cada inventoryItem, por su parte, está compuesto por el identificador del producto al que hace referencia (productid), el catálogo del producto al cual se está refiriendo, ya que un producto puede estar en varios catálogos a la vez, y tener diferente cantidad de stock en cada uno de ellos, por lo que hacen falta diferentes inventoryItem para cada uno de los catálogos en los que este un producto. Además, cada inventoryItem dispone, lógicamente, de la cantidad de stock disponible (quantity), la cual actualizo cada vez que se realicen compras de los productos (reducir stock), y cada vez que se añaden unidades de los productos en los inventarios (aumentar stock). Por último, cada inventoryItem dispone de su fecha de creación (creationDate).

### cart-service
Este microservicio lo he creado para que gestione toda la lógica relacionada con los carritos de los usuarios de las tiendas electrónicas de mis clientes. Las peticiones HTTP a las que responde este microservicio son obtener todos (pudiendo filtrar y ordenar por parámetros) un carrito especifico de la tienda electrónica en cuestión, crear o eliminar el carrito de un usuario, eliminar un ítem de un carrito de un usuario, añadir una cantidad de unidades de un producto al carrito de un usuario (la cantidad de dicho producto ha de ser menor o igual al stock de ese producto en su correspondiente inventario, ya que cada ítem de un carrito (producto + cantidad) tiene asociado un inventario), y hacer el checkout del carrito de un usuario, pudiendo añadir un código de descuento. Cabe destacar que si el precio total del carrito es menor a 30.0 EUR, se añadirá un plus de 2.9 EUR al precio final del pedido en cuestión, en concepto de envío, como ya muestro en mi API Gateway.
Esta última es de las funcionalidades más complejas del sistema, ya que he hecho que se comunique con muchos otros microservicios, además de comunicarse con la API oficial de PayPal, para realizar el pago. La lógica de dicho checkout la he separado en dos partes, checkout1, que se realiza antes del pago del usuario, y checkout2, la cual llamo automáticamente una vez el usuario ha realizado el pago con éxito. 
Un carrito (cart) está formado por un id (único), una lista de ítems (cartItems) y la fecha de creación (creationDate). Cada cartItem, por sus parte, está compuesto por un productId (el identificador del producto del ítem del carrito en cuestión), una cantidad (quantity), que especifica la cantidad de unidades del producto identificado por el productId añadidas al carrito, un inventoryId, que especifica el inventario desde el cual se ha añadido el producto al carrito (esto es clave porque se revisará este inventario constantemente para verificar que hay unidades suficientes en stock), y la disponibilidad (available) del cartItem, el cual tendrá valor true si hay suficiente stock en el inventario identificado por el inventoryId, o false si no hay stock suficiente para satisfacer la cantidad (quantity) de unidades especificada para el cartItem en dicho inventario. Cabe destacar que actualizo el atributo available en tiempo real, es decir, los usuarios de las tiendas electrónicas podrán ver en todo momento si sus carritos se encuentran disponibles (hay suficiente stock) o no. Si no lo están, no podrán realizar el checkout. Por último, cada cartItem dispone de la fecha de creación (creationDate).

### order-service
Este microservicio lo he creado para que maneje la información relativa a los pedidos de los usuarios de mis clientes. Este se encargará de almacenar los productos y las cantidades de cada uno de los carritos que los usuarios han comprado (han hecho checkout), debido a que una vez se hace checkout de un carrito, es comportamiento común que el carrito se vacíe, y, lógicamente, en algún lago se ha de almacenar la compra. Los pedidos (orders en inglés) también están enlazados a un envió (delivery en inglés), del cual hablaré a continuación. Las peticiones HTTP a las que responde mi microservicio order-service son las relativas a la consulta de todos (pudiendo filtrar y ordenar por parámetros) o uno de los carritos de la tienda electrónica en cuestión, crear o eliminar un pedido y enlazar un pedido con un envío a domicilio, es decir, con un documento del microservicio delivery-service.
Un pedido (order) está formado por un id (único), un deliveryId (el identificador del envió enlazado al envió), la información del carrito asignado al pedido (cart) (nótese que guardo la información del carrito en el momento de realizar el pedido, ya que, al terminar el checkout, vacío el carrito en cuestión, y si no guardase la información de los ítems del carrito y solo mantuviera una referencia al carrito la información de este en el momento de la realización del pedido se perdería por completo, y la fecha de creación (creationDate).

### delivery-service
Este microservicio se encarga de gestionar la lógica de los envíos de los pedidos de los usuarios de una tienda electrónica. Concretamente, gestiona información relativa a la dirección de entrega del usuario en el momento de la realización del pedido, ya que esta puede cambiar a lo largo del tiempo, la empresa transportista externa que se responsable de una determinada entrega, el estado en el que se encuentra un envío determinado y la fecha aproximada de entrega de cada uno de los envíos. 
Las peticiones HTTP que acepta este microservicio son las relativas a la consulta de todos (pudiendo filtrar y ordenar por parámetros) o alguno de los envíos de una tienda electrónica, la creación o eliminación de un pedido, la actualización del estado en el que se encuentra un envío y la actualización de la fecha estimada de la entrega de un envío en concreto. Todas estas llamadas serán realizadas por las personas transportistas de las empresas externas de reparto, a las cuales les facilitaré información sobre cómo realizar dichas peticiones HTTPS.
Un envío (delivery) está formado por un id (único), un orderId (el identificador el pedido enlazado al envío), una dirección de entrega (deliveryAddress), el estado del envio (deliveryState), el cual puede ser pendingToSend, alreadySent, arrived o finished, la compañía encargada del envío (deliveryComany), la fecha estimada de entrega (estimatedDateOfArrival), y la fecha de creación (creationDate).

### discount-service
Este microservicio tiene la función de gestionar los códigos de descuentos de una tienda electrónica. Concretamente, responde a las funcionalidades de consultar todos (pudiendo filtrar y ordenar por parámetros) o uno de los descuentos de una tienda electrónica determinada, crear o eliminar un descuento determinado de la BBDD, aumentar el número de usos de un descuento y activar o desactivar un descuento.
Es necesario añadir que he definido la clase Descuento con los atributos siguientes: el id, el code (código del descuento), que es único, el booleano percentage, lo he creado para indicar si se descuenta un porcentaje sobre el precio total de la compra (si vale true), por ejemplo, el 15% del precio total, o si simplemente reduce una cantidad económica determinada en euros (si vale false), por ejemplo 5 euros sobre el precio total, el atributo double value, que indica el valor del descuento, puede referirse a un porcentaje o a un valor en euros, en función del valor de percentage previamente visto, el atributo mínimumAmount, que indica el precio mínimo de la compra para poder utilizar el descuento en cuestión, los atributos startDate y endDate, que indican la fecha inicial y final de validez del descuento, respectivamente, el atributo currentUses, que indica el número de usos total del descuento por los usuarios de la tienda electrónica. Como he comentado anteriormente, el microservicio discount-service dispone de un endpoint el cual, una vez lo llamo, incrementa el número de usos del descuento. Este endpoint lo llamo cuando se hace el checkout de un carrito usando un código de descuento, el atributo maxUses, que indica el número máximo de veces que se puede utilizar un determinado descuento, el atributo booleano enabled, que indica si un descuento está habilitado o no. Este atributo lo he introducido porque me he dado cuenta de que en muchas eCommerce un descuento se suele deshabilitar cunado su funcionamiento no es el previsto, es decir, cuando aparece algún bug en él y los usuarios se aprovechan masivamente de él, el atributo users, que es una lista que contiene las ID’s de los usuarios que se pueden beneficiar del descuento. Si la lista tiene como valor null, querrá decir que todos los usuarios con rol “USER” del sistema se podrán beneficiar del descuento, y la fecha de creación (creationDate).

### wishlist-service
Este microservicio se encarga de gestión la lógica relacionada con las listas de deseos de los usuarios de las tiendas electrónicas de mis clientes. Concretamente, dispone de las funcionalidades de consultar todas (pudiendo filtrar y ordenar por parámetros) o una de las listas de deseos de los usuarios de una determinada tienda electrónica, crear o eliminar una lista de deseos, al reducir el precio de un producto mediante el microservicio product-service notificar automáticamente a todos los usuario los cuales tenían en sus listas de deseos dicho producto y la reducción de precio satisfaga el precio objetivo especificado por los determinados usuarios, añadir un ítem en la wishlist de un usuario y eliminar un ítem de la wishlist de un usuario.
Una wishlist está formada por un id (único), una lista de wishlist ítems (los diferentes ítems de la lista de deseos) y de su fecha de creación. Cada uno de los wishlist ítem está formado, a su vez, por el productId del producto al que hacen referencia, el targetPrice (precio deseado del producto) y la fecha de creación (creationDate).

### account-service
Este microservicio es el encargado de gestionar toda la información correspondiente a los usuarios de cada una de las tiendas electrónicas de mis usuarios. Como se puede intuir, la seguridad es uno de los aspectos clave sobre los que he creado y construido este microservicio. Las peticiones HTTP a las que responde este microservicio son las correspondientes a la consulta de todos (pudiendo filtrar y ordenar por parámetros) o uno de los usuarios de una tienda electrónica, la creación y eliminación de un usuario, el cambio de la dirección de entrega de un usuario y el envío de notificaciones mediante correos electrónicos a los usuarios de una tienda electrónica.
Concretamente, se envían emails informativos a un usuario cuando se producen actualizaciones en el estado de alguno de los envíos de sus pedidos, cuando se actualiza la fecha de entrega de alguno de los envíos de sus pedidos, cuando un pedido se realiza correctamente (es decir, cuando el checkout y el pago mediante PayPal se realizan con éxito), cuando un usuario es elegible para el uso de un nuevo código de descuento, cuando un determinado descuento al que es accesible un usuario se desactiva o activa, y cuando alguno de los productos que un usuario tiene en su lista de deseos (wishlist en inglés) llega al precio que el usuario en cuestión ha especificado como precio objetivo.
Las propiedades de una cuenta de usuario son la id, el nombre del usuario (username), que al igual que el id, es único, el correo electrónico del usuario, que también es único, la dirección de envió el usuario, el rol (role), que identifica si el usuario es administrador (ADMIN) o usuario de la tienda electrónica (USER), y la fecha de creación (creationDate). Esta distinción es clave, ya que mi sistema será utilizado tanto por mis clientes como por sus respectivos usuarios. Por último, tengo, lógicamente, la contraseña del usuario. La contraseña (password) la encripto usando la clase BCryptPasswordEncoder de la librería org.springframework.security.crypto.bcrypt de Spring, usando la función encode, que utiliza el algoritmo BCrypt. Con esto lo que consigo es que las contraseñas se codifiquen mediante la función fuerte de hasheado BCrypt, antes de ser almacenadas. Al hacer esto, como todas las contraseñas de las cuentas las tengo almacenadas hasheadas, no hay manera de obtener la contraseña original a partir del hash, ya que no se han encriptado, se han codificado mediante un hash, por lo que ni yo mismo ni ningún hacker malicioso podría acceder a las contraseñas originales si hipotéticamente acceden a mi sistema. Con esto, tanto mis clientes como sus respectivos usuarios pueden estar seguros de que sus contraseñas solo serán sabidas por ellos mismos. Pero te preguntaras, ¿si las contraseñas se almacenan hasheadas, como un usuario puede iniciar sesión en su cuenta para obtener un token de acceso? La respuesta es simple. Los clientes se tendrán que identificar mediante username y password, y una vez realicen la petición de acceso, comprobar que las credenciales son correctas hasheando de la misma manera la contraseña dada y viendo si coincide con la almacenada. Si lo hacen, querrá decir que las credenciales son correctas y les otorgare su respectivo token JWT.
Cabe destacar que BCrypt lleva usándose desde el 1999, cuando se creó, y que, desde entonces, a pesar de atraer a curiosos maliciosos, el algoritmo sigue sin ser roto.

### resource-service
Este microservicio se encarga de gestionar y almacenar todos los recursos gráficos de los que dispone una tienda electrónica. Estos se componen, básicamente, de las imágenes y/o videos de los productos ofrecidos por la tienda electrónica. He decidido crear este microservicio debido a que pienso que una tienda electrónica seria no ha de depender de enlaces a repositorios de imágenes externos a ella, es decir, que el propio sistema backend de las tiendas electrónicas de mis clientes ha de almacenar todos los recursos gráficos de dichas tiendas. De esta manera, mis clientes no dependerán de la disponibilidad de páginas externas que proporcionen imágenes propias. Mi sistema se encargará de ofrecer dichas imágenes, haciendo que estén disponibles en todo momento para mis clientes. Estos recursos los tengo almacenados encriptados en String Base64. Cuando se acceden a ellos, lo que hago es desencriptarlos antes de enviarlos a los destinatarios. Este sistema de encriptación es usado por cientos de empresas que proporcionan contenido visual, de esta manera se aseguran que los datos de los recursos visuales permanecen intactos durante su transporte mediante llamadas HTTP entre mis microservicios. Si no se encriptasen los datos de los recursos gráficos, estos se transportarían por la red en formato original, y dichos recursos, como las imágenes jpg, por ejemplo, están compuestas por bytes representados por caracteres especiales, que pueden ser malinterpretados por distintos protocolos de red, como caracteres de control, interpretando un salto de línea “\n” un carácter que en realidad intenta representar la propia imagen, lo que llevaría a que los recursos de mis clientes se pudieran ver de manera deformada en depende que dispositivos sus usuarios usen para verlos. Encriptándolos en series formadas por los mismos 64 caracteres me aseguro de que estos no serán nunca malinterpretados. Este aspecto parece un detalle sin importancia, pero es fundamental para cualquier sistema que trabaje con recursos gráficos.
Concretamente, mi microservicio resource-service ofrece las funcionalidades de consultar todos (pudiendo filtrar y ordenar por parámetros) o uno de los recursos visuales de una tienda electrónica, crear o eliminar un recurso visual de una tienda electrónica, y descargar un determinado recurso visual.
Cabe destacar que un recurso grafico está formado por un atributo id (identificador), un atributo name, el nombre del recurso, que es único, un atributo description, una descripción, un atributo data, que contiene el recurso grafico en formato String Base64, y la fecha de creación (creationDate). Para encriptar y desencriptar los recursos gráficos utilizo la librería de Apache “org.apache.commons.codec.binary.Base64”.

### review-service
Este microservicio se encarga de gestionar las funcionalidades relacionadas con las opiniones y valoraciones que los usuarios de las tiendas electrónicas de mis clientes dejan sobre los artículos comprados. Estas funcionalidades, concretamente, son obtener todas las reseñas (reviews) de una tienda electrónica (pudiendo filtrar y ordenar por parámetros), obtener una reseña por su id, crear una reseña, eliminar una reseña, editar el comentario de una reseña, editar el valor de las estrellas de una reseña y dar like a una reseña, donde un usuario no puede dar like más de una vez a la misma reseña.
Una review está formada por su id (único), el id del usuario que la ha realizado (accountId), el id del producto sobre el que se ha hecho la reseña (productId), el comentario de la reseña (comment), las estrellas que el usuario le ha dado al producto en la reseña (stars), el número de likes que la reseña ha recibido (likes), la lista de usuarios que han dado like a la reseña (likers), y la fecha de creación (creationDate).

### auth-service
Este microservicio se encarga aplicar el patrón autenticación en mi sistema, gestionando la autenticación básica (username + password) de mis clientes, de sus administradores de sistema y de los usuarios de su respectiva tienda electrónica. Concretamente, ofrece la funcionalidad de iniciar sesión pasando en formato JSON el username y la contraseña. Si la identificación se realiza con existo, se devolverá un token JWT para acceder a la API de la tienda electrónica en cuestión que les ofrezco, válido para las siguientes 24 horas.

### google-auth-service
Este microservicio lo he creado para ofrecer una alternativa a la autenticación tradicional (username + password), integrando la autenticación mediante Google, usando el seguro protocolo de estándar industrial OAuth 2.0. Para autentificarse, mis clientes y sus empleados serán redirigidos a la página de autenticación de Google. Una vez se identifiquen correctamente mediante su respectiva cuenta de Google, yo les ofreceré un token JWT valido por 24 horas, igual que si se estuviese utilizando la autenticación básica.

### api-gateway-service
Este microservicio es la puerta de entrada a mi sistema de microservicios. Este redireccionará las peticiones que le lleguen a alguna instancia activa del microservicio que se ha de encargar de responderla. Por ejemplo, si llega una petición con subpath “/api/latest/catalogs”, la redireccionará a alguna de las instancias (pods) de mi microservicio catalog. Cabe destacar, pero, que este microservicio no dejara pasar las peticiones que no tengan en la cabecera “Authorization” un token JWT vigente y valido.

### paypal-gateway-service
En este microservicio he implementado toda la gestión relativa al pago de los usuarios mediante PayPal, con el fin de integrar mi sistema con PayPal, una plataforma famosa por la seguridad que ofrece y el apoyo que ofrece, mediante reembolsos, por ejemplo, a sus usuarios cuando sus compras no terminan siendo como esperaban, cosa que aumenta la fiabilidad y confianza de los usuarios a la hora de realizar pagos mediante internet. Este microservicio dispone de un endpoint para la creación del pago mediante PayPal (creación del enlace para redireccionar al usuario a la página de pago de PayPal), un endpoint para verificar el pago, un endpoint al cual se redirigirá una vez el usuario realice el pago con éxito y un endpoint al cual se redirigirá si el usuario en cuestión ha cancelado el pago o este no ha tenido éxito. Cabe destacar que estos endpoints están pensados para que sean accedidos por mi microservicio o una vez los usuarios de mis clientes realicen el pago mediante la pasarela de pago de PayPal, por lo que no los expongo en mi API Gateway, por temas de seguridad. Estas se pueden ver en más detalle en mi documentación Swagger https://app.swaggerhub.com/apis-docs/scaredstew/eCommerceSaaS. El código de este está disponible en GitHub https://github.com/MohamedAmarani/microservices/tree/main/paypal-gateway-service.

## Ejemplos de notificaciones via email

![etqeqr](https://user-images.githubusercontent.com/61777025/133081671-f1a6d8e3-f018-48e3-9cbf-965ad7dd5e5b.png)

