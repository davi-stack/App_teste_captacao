import { Alert } from 'react-native';
import { pickSingle } from 'react-native-document-picker';
import RNFS from 'react-native-fs';
import axios from 'axios';

const SERVER_URL = 'http://54.233.209.5:8080/teste';

// async function requestStoragePermission(): Promise<boolean> {
//   if (Platform.OS === 'android' && Platform.Version >= 23) {
//     const granted = await PermissionsAndroid.request(
//       PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
//       {
//         title: 'Permissão de armazenamento',
//         message: 'O app precisa acessar seus arquivos para enviar o PDF.',
//         buttonPositive: 'Permitir',
//       }
//     );
//     return granted === PermissionsAndroid.RESULTS.GRANTED;
//   }
//   return true;
// }

export const uploadPdfFile = async () => {
  try {
    // const hasPermission = await requestStoragePermission();
    // if (!hasPermission) {
    //   Alert.alert('Permissão negada', 'Você precisa permitir acesso aos arquivos.');
    //   return;
    // }

    const file = await pickSingle({
      type: ['application/pdf'],
      copyTo: 'cachesDirectory',
    });

    const fileUri = file.fileCopyUri || file.uri;
    const path = fileUri.startsWith('file://') ? fileUri : `file://${fileUri}`;

    const exists = await RNFS.exists(path.replace('file://', ''));
    if (!exists) {
      Alert.alert('Erro', 'Arquivo não encontrado no caminho selecionado.');
      return;
    }

    const formData = new FormData();
    formData.append('arquivo', {
      uri: path,
      name: file.name || 'arquivo.pdf',
      type: 'application/pdf',
    } as any);

    const response = await axios.post(SERVER_URL, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    Alert.alert('Sucesso', response.data.mensagem || 'PDF enviado com sucesso!');
  } catch (err: any) {
    console.error(err);
    Alert.alert('Erro', err?.message || 'Falha ao enviar o PDF');
  }
};
