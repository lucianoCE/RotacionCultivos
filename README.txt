GRUPO M

Indicaciones:
- Las instancias a ejecutar se deben guardar en un archivo xml en el directorio src/main/resources/instancias.

Es posible ejecutar una instancia una única vez a través del main:
- Al ejecutar el programa el usuario debe indicar el nombre de la instancia a evaluar (incluyendo extension), si quiere que se generen graficos de las soluciones y si se quiere generar un archivo excel para las soluciones.
- Todos los archivos generados se guardan en el directorio '/resultados/<nombre_instancia>/'.
- Los resultados de los greedys se guardan en archivos .csv bajo el nombre 'greedy_<objetivo>_results' en el directorio principal.

En la clase 'ParetoEstimate' se hace una ejecución de 30 veces de la instancia deseada, donde se realiza el análisis estádistico de los resultados (hay que modificar la clase e indicar la instancia correspondiente, por default viene con las tres instancias precargadas (instanciaChica, instanciaMediana e instanciaGrande) utilizadas para el proyecto).
Esta clase retorna los resultados printeados en la consola. A su vez, las imagenes resultados son almacenadas en la carpeta '/frentes_pareto'.

En la clase 'ParameterTuning' se realizan 

Esta es la entrega final.