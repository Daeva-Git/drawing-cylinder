#version 330 core
out vec4 FragColor;

in vec3 Normal;
in vec3 FragPos;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;

void main()
{
    // Ka ambient reflection coefficient
    float Ka = 0.5;
    // Kd diffuse-reflection coefficient
    float Kd = 1;
    // Ks specular reflection coefficient
    float Ks = 2;
    // k shininess factor
    float k = 32;

    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    vec3 viewDir = normalize(viewPos - FragPos);
    vec3 reflectDir = reflect(lightDir, norm);

    // ambient
    vec3 ambient = Ka * lightColor;

    // diffuse
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = Kd * diff * lightColor;

    // specular
    float spec = pow(max(dot(Normal, normalize(viewDir + lightDir)), 0.0), k);
    vec3 specular = Ks * spec * lightColor;

    vec3 result = (ambient + diffuse + specular) * objectColor;
    FragColor = vec4(result, 1.0);
}