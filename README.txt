GRUPO M

Indicaciones:
- Las instancias a ejecutar se deben guardar en un archivo xml en el directorio src/main/resources/instancias.

Es posible ejecutar una instancia una única vez a través del Main:
- Al ejecutar el programa el usuario debe indicar el nombre de la instancia a evaluar (incluyendo extension), si quiere que se generen graficos de las soluciones y si se quiere generar un archivo excel para las soluciones.
- Todos los archivos generados se guardan en el directorio '/resultados/<nombre_instancia>/'.
- Los resultados de los greedys se guardan en archivos .csv bajo el nombre 'greedy_<objetivo>_results' en el directorio principal.

En la clase 'ParetoEstimate' se hace una ejecución de 30 veces de la instancia deseada, donde se realiza el análisis estádistico de los resultados (hay que modificar la clase e indicar la instancia correspondiente, por default viene con las tres instancias precargadas (instanciaChica, instanciaMediana e instanciaGrande) utilizadas para el proyecto).
Esta clase retorna los resultados printeados en la consola. A su vez, las imagenes resultados son almacenadas en la carpeta '/frentes_pareto'.

En la clase 'ParameterTuning' se realiza la configuración parametrica, evaluando combinaciones de tamaño de población, número de evaluaciones y probabilidad de cruce. 
Ejecuta cada combinación 30 veces, generando un análisis estadístico de los resultados, como el beneficio promedio, diversidad, hipervolumen y tiempo de ejecución. 
Los resultados se almacenan en un archivo CSV.

Las clases ScatterPlot y ExcelExporter son auxiliares que facilitan la visualización y exportación de resultados de optimización. 
- 'ScatterPlot' se encarga de generar gráficos de dispersión utilizando la librería JFreeChart. Permite guardar los gráficos como imágenes. 
- 'ExcelExporter' facilita la exportación de los datos de soluciones a archivos Excel

Los scripts en Python diseñados, utilizan las bibliotecas Matplotlib y Pandas, son para la generación de los gráficos basados en los resutlados obtenidos de la configuración paramétrica.


Esta es la entrega final.