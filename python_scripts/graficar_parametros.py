import pandas as pd
import matplotlib.pyplot as plt
import os

# Cargar el archivo CSV
def cargar_datos(archivo_csv):
    # Leer los datos del archivo CSV con separador ";"
    return pd.read_csv(archivo_csv, delimiter=";")

# Graficar los datos según los criterios
def graficar_datos(data, columna_y, nombre_y, archivo_salida_base):
    # Filtrar valores únicos de CrossProb
    cross_probs = data['CrossProb'].unique()
    pop_sizes = data['PopSize'].unique()
    
    for cross_prob in cross_probs:
        # Filtrar datos para cada CrossProb
        subset = data[data['CrossProb'] == cross_prob]
        
        # Crear el gráfico
        plt.figure(figsize=(12, 6))
        
        for pop_size in pop_sizes:
            # Filtrar datos para cada tamaño de población
            pop_subset = subset[subset['PopSize'] == pop_size]
            plt.plot(pop_subset['MaxEvals'], pop_subset[columna_y], marker='o', label=f'PopSize: {pop_size}')
        
        # Configurar el gráfico
        plt.title(f'{nombre_y} vs MaxEvals (CrossProb: {cross_prob})')
        plt.xlabel('MaxEvals')
        plt.ylabel(nombre_y)
        plt.legend()
        plt.grid(True)
        
        # Guardar el gráfico como imagen
        archivo_salida = f"{archivo_salida_base}_CrossProb_{cross_prob}.png"
        plt.savefig(archivo_salida)
        print(f'Gráfico guardado como: {archivo_salida}')
        plt.close()

# Función principal
def main():
    try:
        # Cargar los datos
        datos = cargar_datos("parameter_tuning_summary.csv")
        
        # Crear gráficos para AvgTime
        graficar_datos(
            datos, 
            columna_y='AvgTime', 
            nombre_y='Tiempo Promedio (s)', 
            archivo_salida_base='grafico_tiempo_promedio'
        )
        
        # Crear gráficos para AvgHypervolume
        graficar_datos(
            datos, 
            columna_y='AvgHypervolume', 
            nombre_y='Hipervolumen Promedio', 
            archivo_salida_base='grafico_hipervolumen_promedio'
        )

        # Crear gráficos para AvgProfit
        graficar_datos(
            datos, 
            columna_y='AvgProfit', 
            nombre_y='Ganancia Promedio', 
            archivo_salida_base='grafico_ganancia_promedio'
        )

        # Crear gráficos para AvgDiversity
        graficar_datos(
            datos, 
            columna_y='AvgDiversity', 
            nombre_y='Diversidad Promedio', 
            archivo_salida_base='grafico_diversidad_promedio'
        )
        
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()
